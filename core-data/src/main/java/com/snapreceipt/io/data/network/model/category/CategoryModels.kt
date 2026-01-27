package com.snapreceipt.io.data.network.model.category

/**
 * 分类列表请求（/api/category/list，无参数）。
 */
class CategoryListRequestDto

/**
 * 分类新增请求（/api/category/remove，后端实际路径以接口为准）。
 *
 * @property categoryName 分类名称
 */
data class CategoryCreateRequestDto(
    val categoryName: String
)

/**
 * 分类删除请求（/api/category/remove）。
 *
 * @property categoryIds 分类ID列表
 */
data class CategoryDeleteRequestDto(
    val categoryIds: List<Long>
)

/**
 * 分类条目（/api/category/list 返回的 data 列表项）。
 *
 * @property createBy 创建人
 * @property createTime 创建时间
 * @property updateBy 更新人
 * @property updateTime 更新时间
 * @property remark 备注
 * @property categoryId 分类ID
 * @property userId 用户ID（0 表示后台默认分类）
 * @property categoryName 分类名称
 * @property categoryIcon 分类图标（备用）
 * @property orderNum 排序
 * @property status 状态
 * @property delFlag 删除标记
 */
data class CategoryItemDto(
    val createBy: String? = null,
    val createTime: String? = null,
    val updateBy: String? = null,
    val updateTime: String? = null,
    val remark: String? = null,
    val categoryId: Long,
    val userId: Long? = null,
    val categoryName: String,
    val categoryIcon: String? = null,
    val orderNum: Int? = null,
    val status: String? = null,
    val delFlag: String? = null
)
