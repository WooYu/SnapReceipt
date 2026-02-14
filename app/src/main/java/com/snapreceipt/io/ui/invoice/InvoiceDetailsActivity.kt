package com.snapreceipt.io.ui.invoice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.observeState
import com.skybound.space.core.network.auth.SessionEvent
import com.skybound.space.core.network.auth.SessionManager
import com.skybound.space.core.util.DateFormatUtil
import com.snapreceipt.io.MainActivity
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivityInvoiceDetailsBinding
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.ui.invoice.bottomsheet.InvoiceCategoryBottomSheet
import com.snapreceipt.io.ui.invoice.bottomsheet.TitleTypeBottomSheet
import com.snapreceipt.io.ui.login.LoginActivity
import com.snapreceipt.io.ui.main.ListRefreshViewModel
import com.snapreceipt.io.ui.widget.datepicker.DateTimePickerBottomSheet
import com.snapreceipt.io.util.ReceiptTypeHelper
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.launch
import androidx.activity.OnBackPressedCallback

@AndroidEntryPoint
class InvoiceDetailsActivity : BaseActivity<InvoiceDetailsViewModel>() {
    companion object {
        fun createIntent(
            context: Context, 
            receipt: ReceiptEntity, 
            source: String = InvoiceDetailsArgsCodec.SOURCE_RECEIPTS_LIST
        ): Intent {
            return Intent(context, InvoiceDetailsActivity::class.java).apply {
                InvoiceDetailsArgsCodec.writeReceipt(this, receipt)
                putExtra(InvoiceDetailsArgsCodec.EXTRA_SOURCE_SCENE, source)
            }
        }
    }

    override val viewModel: InvoiceDetailsViewModel by viewModels()

    @Inject
    lateinit var injectedSessionManager: SessionManager

    @Inject
    lateinit var receiptTypeHelper: ReceiptTypeHelper

    override val sessionManager: SessionManager
        get() = injectedSessionManager

    private var _binding: ActivityInvoiceDetailsBinding? = null
    private val binding get() = _binding!!

    // 来源场景：扫描或列表
    private var sourceScene: String = InvoiceDetailsArgsCodec.SOURCE_RECEIPTS_LIST

    private var receiptImagePath: String = ""
    private var receiptImageUrl: String = ""
    private var receiptDate: String = ""
    private var receiptTime: String = ""
    private var scanConsumer: String = ""
    private var scanTipAmount: Double? = null
    private var receiptId: Long? = null
    private var isEditing: Boolean = false
    
    private var initialCategoryId: Long? = null
    private var initialCategoryLabel: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityInvoiceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()

        // 读取来源场景参数
        sourceScene = intent.getStringExtra(InvoiceDetailsArgsCodec.EXTRA_SOURCE_SCENE) ?: InvoiceDetailsArgsCodec.SOURCE_RECEIPTS_LIST

        val receipt = InvoiceDetailsArgsCodec.readReceipt(intent)
        val rawImage = receipt.receiptUrl.orEmpty().trim()
        resolveImageSource(rawImage)
        receiptId = receipt.receiptId
        // 根据来源场景决定初始状态：
        // - 扫描来源: 直接进入编辑状态
        // - 列表来源: 根据是否已保存决定（已保存为预览，未保存为编辑）
        isEditing = sourceScene == InvoiceDetailsArgsCodec.SOURCE_SCAN
        initialCategoryId = receipt.categoryId?.takeIf { it > 0L }
        initialCategoryLabel = receipt.categoryName.orEmpty().trim()
        loadInvoiceImage()

        val amountText = receipt.totalAmount?.let { formatAmount(it) }.orEmpty()
        binding.inputAmount.setText(amountText)
        bindReadonlyAmount(amountText)
        val merchantText = receipt.merchant.orEmpty()
        val addressText = receipt.address.orEmpty()
        binding.inputMerchant.setText(merchantText)
        binding.inputAddress.setText(addressText)
        binding.valueMerchant.text = readonlyText(merchantText)
        binding.valueAddress.text = readonlyText(addressText)
        receiptDate = receipt.receiptDate.orEmpty()
        receiptTime = receipt.receiptTime.orEmpty()
        val displayDate = buildDisplayDate(receiptDate, receiptTime)
        binding.inputDate.setText(displayDate)
        binding.valueDate.text = readonlyText(displayDate)
        val cardText = receipt.paymentCardNo.orEmpty()
        binding.inputCard.setText(cardText)
        binding.valueCard.text = readonlyText(cardText)
        scanConsumer = receipt.consumer.orEmpty()
        scanTipAmount = receipt.tipAmount
        setInvoiceCategorySelection(receipt.categoryName.orEmpty())
        setTitleTypeSelection(receipt.receiptType.orEmpty())
        val noteText = receipt.remark.orEmpty()
        binding.inputNote.setText(noteText)
        binding.valueNote.text = readonlyText(noteText)

        binding.pageTitle.setOnLeftIconClickListener { handleBackNavigation() }
        binding.pageTitle.setOnRightIconClickListener { onTopRightActionClick() }
        binding.invoiceImage.setOnClickListener { openImagePreview() }

        renderModeUi()
        setupPickers()
        setupCardValidation()
        binding.saveBtn.setOnClickListener { saveReceipt(receiptImagePath) }
        observeState(viewModel.uiState) { renderState(it) }
        
        // 使用现代 OnBackPressedCallback 替代过时的 onBackPressed()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })
    }

    override fun onDestroy() {
        _binding?.pageTitle?.setOnLeftIconClickListener(null)
        _binding?.pageTitle?.setOnRightIconClickListener(null)
        _binding?.invoiceImage?.setOnClickListener(null)
        _binding?.saveBtn?.setOnClickListener(null)
        _binding = null
        super.onDestroy()
    }

    private fun renderState(state: InvoiceDetailsUiState) {
        binding.saveBtn.isEnabled = !state.loading
        if (state.loading) {
            showGlobalLoading(null)
        } else {
            hideGlobalLoading()
        }
    }

    private fun handleBackNavigation() {
        if (isEditing && sourceScene != InvoiceDetailsArgsCodec.SOURCE_SCAN) {
            // 编辑状态返回到预览状态
            isEditing = false
            renderModeUi()
        } else {
            // 预览状态或扫描场景返回到列表（不刷新）
            finish()
        }
    }

    private fun renderModeUi() {
        val showEdit = isEditing
        binding.amountEditContainer.visibility = if (showEdit) View.VISIBLE else View.GONE
        binding.amountReadonlyContainer.visibility = if (showEdit) View.GONE else View.VISIBLE
        binding.sectionEditMode.visibility = if (showEdit) View.VISIBLE else View.GONE
        binding.sectionViewMode.visibility = if (showEdit) View.GONE else View.VISIBLE
        binding.viewNoteLabel.visibility = if (showEdit) View.GONE else View.VISIBLE
        binding.valueNote.visibility = if (showEdit) View.GONE else View.VISIBLE
        binding.bottomActionContainer.visibility = if (showEdit) View.VISIBLE else View.GONE

        val isSaved = sourceScene != InvoiceDetailsArgsCodec.SOURCE_SCAN
        binding.pageTitle.setRightIconVisible(isSaved)
        if (isSaved) {
            if (showEdit) {
                binding.pageTitle.setRightIcon(R.drawable.ic_trash_white)
            } else {
                binding.pageTitle.setRightIcon(R.drawable.ic_edit_white)
            }
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun applyWindowInsets() {
        val initialScrollBottomPadding = binding.contentScroll.paddingBottom
        val initialBottomActionPadding = binding.bottomActionContainer.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navigationBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottomInset = max(navigationBottom, imeBottom)

            val scrollBottomInset = if (binding.bottomActionContainer.visibility == View.VISIBLE) {
                0
            } else {
                bottomInset
            }
            binding.contentScroll.updatePadding(bottom = initialScrollBottomPadding + scrollBottomInset)
            binding.bottomActionContainer.updatePadding(bottom = initialBottomActionPadding + bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun onTopRightActionClick() {
        val isSaved = sourceScene != InvoiceDetailsArgsCodec.SOURCE_SCAN
        if (!isSaved) return
        if (isEditing) {
            // 编辑状态下点击删除
            deleteReceiptIfNeeded()
            return
        }
        // 预览状态下进入编辑
        isEditing = true
        renderModeUi()
    }

    override fun onCustomEvent(event: UiEvent.Custom) {
        when (event.type) {
            InvoiceDetailsEventKeys.SHOW_SUCCESS -> {
                Toast.makeText(this, getString(R.string.receipt_save_success), Toast.LENGTH_SHORT)
                    .show()
            }

            InvoiceDetailsEventKeys.NAVIGATE_TO_MAIN -> {
                // 保存成功，返回列表并告知操作类型（新增或编辑）
                val isNewReceipt = sourceScene == InvoiceDetailsArgsCodec.SOURCE_SCAN
                val operationType = if (isNewReceipt) InvoiceDetailsArgsCodec.OPERATION_TYPE_ADD else InvoiceDetailsArgsCodec.OPERATION_TYPE_UPDATE
                
                // 读取编辑后的数据，返回给 Fragment 用于本地更新
                val editedReceipt = collectEditedReceipt()
                
                setResult(RESULT_OK, Intent().apply {
                    putExtra(InvoiceDetailsArgsCodec.EXTRA_OPERATION_TYPE, operationType)
                    putExtra(InvoiceDetailsArgsCodec.EXTRA_RECEIPT, editedReceipt)
                    putExtra(InvoiceDetailsArgsCodec.EXTRA_SOURCE_SCENE, sourceScene)
                })
                finish()
            }
            InvoiceDetailsEventKeys.DELETE_SUCCESS -> {
                val id = receiptId ?: return
                setResult(RESULT_OK, Intent().apply {
                    putExtra(InvoiceDetailsArgsCodec.EXTRA_OPERATION_TYPE, InvoiceDetailsArgsCodec.OPERATION_TYPE_DELETE)
                    putExtra(InvoiceDetailsArgsCodec.EXTRA_RECEIPT_ID, id)
                    putExtra(InvoiceDetailsArgsCodec.EXTRA_SOURCE_SCENE, sourceScene)
                })
                finish()
            }
        }
    }

    private fun saveReceipt(imagePath: String) {
        // 0. Amount
        val amountText = binding.inputAmount.text.toString().trim()
        val amountValue = amountText.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            Toast.makeText(this, getString(R.string.validation_amount_empty), Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Merchant
        val merchantValue = binding.inputMerchant.text.toString().trim()
        if (merchantValue.isBlank()) {
            Toast.makeText(this, getString(R.string.validation_merchant_empty), Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Address
        val addressValue = binding.inputAddress.text.toString().trim()
        if (addressValue.isBlank()) {
            Toast.makeText(this, getString(R.string.validation_address_empty), Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Date
        if (receiptDate.isBlank()) {
            Toast.makeText(this, getString(R.string.validation_date_empty), Toast.LENGTH_SHORT).show()
            return
        }

        // 4. Bank Card
        val cardValue = binding.inputCard.text.toString().trim()
        if (cardValue.isBlank()) {
            Toast.makeText(this, getString(R.string.validation_card_empty), Toast.LENGTH_SHORT).show()
            return
        }
        val cardError = cardValidationErrorResId(cardValue)
        if (cardError != null) {
            updateCardHelper(cardValue)
            Toast.makeText(this, getString(cardError), Toast.LENGTH_SHORT).show()
            return
        }

        // 5. Invoice Category
        val invoiceCategoryInput = binding.inputInvoiceCategory.text.toString().trim()
        if (invoiceCategoryInput.isBlank()) {
            Toast.makeText(this, getString(R.string.select_invoice_category), Toast.LENGTH_SHORT).show()
            return
        }

        // 6. Invoice Type
        val titleTypeDisplayValue = binding.inputInvoiceType.text.toString().trim()
        val titleTypeValue = receiptTypeHelper.toPayloadValue(titleTypeDisplayValue)
        if (titleTypeValue.isBlank()) {
            Toast.makeText(this, getString(R.string.select_invoice_type), Toast.LENGTH_SHORT).show()
            return
        }

        val noteValue = binding.inputNote.text.toString().trim()
        val receiptUrl = receiptImageUrl.ifEmpty { imagePath }.takeIf { it.isNotBlank() }
        val safeTime = receiptTime.ifEmpty { "00:00:00" }
        val fallbackCategoryId = initialCategoryId?.takeIf {
            invoiceCategoryInput.equals(initialCategoryLabel, ignoreCase = true)
        }

        val receipt = ReceiptEntity(
            receiptId = receiptId,
            merchant = merchantValue,
            receiptDate = receiptDate,
            receiptTime = safeTime,
            totalAmount = amountValue,
            tipAmount = scanTipAmount,
            paymentCardNo = cardValue,
            consumer = scanConsumer.ifEmpty { titleTypeValue },
            remark = noteValue,
            receiptUrl = receiptUrl,
            categoryId = fallbackCategoryId,
            categoryName = invoiceCategoryInput,
            receiptType = titleTypeValue,
            address = addressValue
        )
        if (sourceScene != InvoiceDetailsArgsCodec.SOURCE_SCAN) {
            viewModel.updateReceipt(receipt)
        } else {
            viewModel.saveReceipt(receipt)
        }
    }

    private fun setupPickers() {
        bindPickerRow(
            container = binding.containerInputInvoiceCategory,
            input = binding.inputInvoiceCategory,
            onClick = ::openInvoiceCategoryPicker
        )
        bindPickerRow(
            container = binding.containerInputInvoiceType,
            input = binding.inputInvoiceType,
            onClick = ::openTitleTypePicker
        )
        bindPickerRow(
            container = binding.containerInputDate,
            input = binding.inputDate,
            onClick = ::openDateTimePicker
        )
    }

    private fun bindPickerRow(
        container: View,
        input: View,
        onClick: () -> Unit
    ) {
        container.setOnClickListener { onClick() }
        input.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { onClick() }
        }
    }

    private fun setupCardValidation() {
        // TODO: temporarily disable validation to show backend values as-is.
        binding.cardHelper.visibility = View.GONE
    }

    private fun updateCardHelper(raw: String) {
        val errorRes = cardValidationErrorResId(raw)
        if (errorRes == null) {
            binding.cardHelper.visibility = View.GONE
            return
        }
        binding.cardHelper.text = getString(errorRes)
        binding.cardHelper.visibility = View.VISIBLE
    }

    private fun cardValidationErrorResId(raw: String): Int? {
        // TODO: validation rules will be re-enabled later.
        return null
    }

    private fun openImagePreview() {
        if (receiptImagePath.isBlank() && receiptImageUrl.isBlank()) return
        val intent =
            Intent(this, com.snapreceipt.io.ui.preview.ImagePreviewActivity::class.java).apply {
                putExtra(
                    com.snapreceipt.io.ui.preview.ImagePreviewActivity.EXTRA_IMAGE_PATH,
                    receiptImagePath
                )
                putExtra(
                    com.snapreceipt.io.ui.preview.ImagePreviewActivity.EXTRA_IMAGE_URL,
                    receiptImageUrl
                )
            }
        startActivity(intent)
    }

    private fun openInvoiceCategoryPicker() {
        InvoiceCategoryBottomSheet.newInstance(binding.inputInvoiceCategory.text.toString()) { selected ->
            setInvoiceCategorySelection(selected)
        }.show(supportFragmentManager, "invoice_category_picker")
    }

    private fun openTitleTypePicker() {
        TitleTypeBottomSheet(binding.inputInvoiceType.text.toString()) { selected ->
            setTitleTypeSelection(selected)
        }.show(supportFragmentManager, "title_type_picker")
    }

    private fun setInvoiceCategorySelection(value: String) {
        val normalized = value.trim()
        binding.inputInvoiceCategory.setText(normalized)
        binding.valueInvoiceCategory.text = readonlyText(normalized)
    }

    private fun setTitleTypeSelection(value: String) {
        val displayLabel = receiptTypeHelper.toDisplayLabel(value)
        binding.inputInvoiceType.setText(displayLabel)
        binding.valueInvoiceType.text = readonlyText(displayLabel)
    }

    private fun openDateTimePicker() {
        val initial = parseDateTime(receiptDate, receiptTime)
        DateTimePickerBottomSheet(initial) { date, time, display ->
            receiptDate = date
            receiptTime = time
            binding.inputDate.setText(display)
            binding.valueDate.text = readonlyText(display)
        }.show(supportFragmentManager, "date_time_picker")
    }

    private fun deleteReceiptIfNeeded() {
        val isSaved = sourceScene != InvoiceDetailsArgsCodec.SOURCE_SCAN
        if (!isSaved) return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setMessage(getString(R.string.delete_receipt_confirm))
            .setPositiveButton(R.string.confirm) { _, _ ->
                val id = receiptId ?: return@setPositiveButton
                viewModel.deleteReceipt(id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun buildDisplayDate(date: String, time: String): String {
        if (date.isBlank()) return ""
        val displayDate = date.replace('-', '/')
        val displayTime = time.takeIf { it.isNotBlank() }?.substring(0, 5).orEmpty()
        return if (displayTime.isNotEmpty()) "$displayDate $displayTime" else displayDate
    }

    /**
     * 收集编辑表单中的所有数据，构建完整的 ReceiptEntity 对象
     * 用于保存成功后返回给 Fragment 进行本地列表更新
     */
    private fun collectEditedReceipt(): ReceiptEntity {
        val amountText = binding.inputAmount.text.toString().trim()
        val merchantValue = binding.inputMerchant.text.toString().trim()
        val addressValue = binding.inputAddress.text.toString().trim()
        val cardValue = binding.inputCard.text.toString().trim()
        val invoiceCategoryInput = binding.inputInvoiceCategory.text.toString().trim()
        val titleTypeDisplayValue = binding.inputInvoiceType.text.toString().trim()
        val titleTypeValue = receiptTypeHelper.toPayloadValue(titleTypeDisplayValue)
        val noteValue = binding.inputNote.text.toString().trim()
        val safeTime = receiptTime.ifEmpty { "00:00:00" }
        val fallbackCategoryId = initialCategoryId?.takeIf {
            invoiceCategoryInput.equals(initialCategoryLabel, ignoreCase = true)
        }
        val receiptUrl = receiptImageUrl.ifEmpty { receiptImagePath }.takeIf { it.isNotBlank() }

        return ReceiptEntity(
            receiptId = receiptId,
            merchant = merchantValue,
            receiptDate = receiptDate,
            receiptTime = safeTime,
            totalAmount = amountText.toDoubleOrNull() ?: 0.0,
            tipAmount = scanTipAmount,
            paymentCardNo = cardValue,
            consumer = scanConsumer.ifEmpty { titleTypeValue },
            remark = noteValue,
            receiptUrl = receiptUrl,
            categoryId = fallbackCategoryId,
            categoryName = invoiceCategoryInput,
            receiptType = titleTypeValue,
            address = addressValue
        )
    }

    private fun formatAmount(amount: Double): String =
        String.format(Locale.US, "%.2f", amount)

    private fun bindReadonlyAmount(amountText: String) {
        val hasAmount = amountText.isNotBlank()
        binding.amountReadonlyValue.text = if (hasAmount) amountText else placeholder()
        binding.amountPrefixView.visibility = if (hasAmount) View.VISIBLE else View.GONE
    }

    private fun placeholder(): String = getString(R.string.placeholder_dash)

    private fun readonlyText(raw: String): String = raw.ifBlank { placeholder() }

    private fun resolveImageSource(rawImage: String) {
        if (rawImage.isBlank()) {
            receiptImagePath = ""
            receiptImageUrl = ""
            return
        }
        if (rawImage.startsWith("http", ignoreCase = true)) {
            receiptImageUrl = rawImage
            receiptImagePath = ""
            return
        }
        receiptImagePath = rawImage
        receiptImageUrl = ""
    }

    private fun loadInvoiceImage() {
        val model = imageModel(receiptImagePath, receiptImageUrl) ?: return
        Glide.with(this)
            .load(model)
            .centerCrop()
            .into(binding.invoiceImage)
    }

    private fun imageModel(imagePath: String, imageUrl: String): Any? {
        localImageModel(imagePath)?.let { return it }
        return imageUrl.takeIf { it.isNotBlank() }
    }

    private fun localImageModel(path: String): Any? {
        if (path.isBlank()) return null
        if (path.startsWith("content://", ignoreCase = true) ||
            path.startsWith("file://", ignoreCase = true)
        ) {
            return Uri.parse(path)
        }
        return File(path).takeIf { it.exists() }
    }

    private fun currentDate(): String =
        DateFormatUtil.todayApiDate()

    private fun parseDateTime(date: String, time: String): Long? =
        DateFormatUtil.parseApiDateTime(date, time)

    override fun onSessionExpired(event: SessionEvent) {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        finish()
    }

}
