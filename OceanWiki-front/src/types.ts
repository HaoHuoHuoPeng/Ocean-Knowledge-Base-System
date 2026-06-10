// 文件说明：这个文件负责统一管理前端 TypeScript 类型，让接口数据结构更清楚。

// 后端统一返回结构
// 所有接口基本都会返回 success、message、content 三个字段
export interface CommonResp<T> {
  // 接口是否处理成功
  success: boolean
  // 后端返回给页面看的提示信息
  message: string
  // 后端真正返回的数据，具体类型由接口决定
  content: T
}

// 批量导入结果结构
// successNames 是导入成功的数据，failureMap 是失败行和原因
export interface ImportResp {
  // 导入成功的数据名称
  successNames: string[]
  // 导入失败的数据，key 是行号或数据标识，value 是失败原因
  failureMap: Record<string, string>
}

// 后端分页返回结构
export interface PageResp<T> {
  // 当前页数据列表
  records: T[]
  // 符合条件的总条数
  total: number
  // 当前页码
  current: number
  // 每页条数
  pageSize: number
}

// 地址参数分页返回结构
// 对应后端 /ebook/listByPage?page=1&size=3 这种接口
export interface ListByPageResp<T> {
  // 符合条件的总条数
  total: number
  // 当前页数据列表
  list: T[]
}

// 业务 ID 类型
// 雪花算法 ID 是 19 位数字，后端会按字符串返回，前端统一用 IdValue 接收
export type IdValue = string | number

// 分类表结构
export interface Category {
  // 分类 id，新增时不传，编辑时传给后端定位要改哪条数据
  id?: IdValue
  // 父分类 id，0 表示一级分类，其它值表示挂到对应父分类下面
  parent: IdValue
  // 分类名称，页面树形分类展示的文字
  name: string
  // 后端自动维护的排序值，前端一般不需要手动填写
  sort?: number
  // 前端树形展示用的子分类列表
  children?: Category[]
}

// 电子书表结构
export interface Ebook {
  // 电子书 id，新增时不传，编辑和删除时用来定位数据
  id?: IdValue
  // 电子书名称
  name: string
  // 当前选中的最终分类 id，多级分类时取最后一级
  categoryId?: IdValue
  // 一级分类 id，兼容旧字段，后续主要看 categoryId
  category1Id?: IdValue
  // 二级分类 id，兼容旧字段，后续主要看 categoryId
  category2Id?: IdValue
  // 电子书简介
  description?: string
  // 电子书封面地址，可以是网络地址，也可以是本地上传后返回的地址
  cover?: string
  // 电子书状态：草稿、待审核、已发布、已驳回、已下架
  status?: 'draft' | 'pending' | 'published' | 'rejected' | 'offline'
  // 下架原因，状态为 offline 时使用
  offlineReason?: string
  // 审核备注，审核驳回或审核通过时展示给编辑人员看
  reviewRemark?: string
  // 文档数量，由后端统计，不需要前端提交
  docCount?: number
  // 阅读数量，由后端统计，不需要前端提交
  viewCount?: number
  // 点赞数量，由后端统计，不需要前端提交
  voteCount?: number
  // 推荐分数，由推荐逻辑计算，不需要前端提交
  recommendScore?: number
}

// 文档表结构
export interface Doc {
  // 文档 id，新增时不传，编辑、删除、回滚时用来定位数据
  id?: IdValue
  // 所属电子书 id
  ebookId?: IdValue
  // 父文档 id，0 表示一级目录，其它值表示子文档
  parent: IdValue
  // 文档标题
  name: string
  // 排序字段，后端会按同级目录自动处理，前端通常不手动填
  sort: number
  // 阅读数量，由后端统计
  viewCount?: number
  // 点赞数量，由后端统计
  voteCount?: number
  // 文档状态：草稿、待审核、已发布、已驳回、已下架
  status?: 'draft' | 'pending' | 'published' | 'rejected' | 'offline'
  // 审核备注，驳回时告诉投稿人原因
  reviewRemark?: string
  // 创建人 id，普通用户投稿时用来记录是谁提交的
  createUserId?: IdValue
  // 文档正文，富文本编辑器生成的 HTML 内容
  content?: string
  // 前端树形展示用的子文档列表
  children?: Doc[]
}

// 文档点赞状态
export interface DocVoteStatus {
  // 当前登录用户是否已经点赞
  voted: boolean
  // 当前文档最新点赞数
  voteCount: number
}

// 用户表结构
export interface User {
  // 用户 id，新增时不传，编辑和删除时用来定位用户
  id?: IdValue
  // 账号，用来登录系统
  loginName: string
  // 用户姓名，页面上展示给别人看的名字
  name: string
  // 密码，新增用户时填写；编辑时为空表示不修改密码
  password?: string
  // 用户拥有的角色 id，新增用户和分配角色时传给后端
  roleIds?: IdValue[]
  // 用户拥有的角色名称，后端返回给页面展示用
  roleNames?: string[]
  // 用户拥有的角色编码，前端用它判断是否超级管理员等身份
  roleCodes?: string[]
  // 用户拥有的权限编码，前端用它控制菜单和按钮是否显示
  permissions?: string[]
}

// 角色表结构
export interface Role {
  // 角色 id，新增时不传，编辑和删除时用来定位角色
  id?: IdValue
  // 角色编码，后端判断权限时主要使用这个值
  code: string
  // 角色名称，页面展示用
  name: string
  // 角色说明，告诉管理员这个角色能做什么
  description?: string
  // 是否内置角色，1 表示系统默认角色，通常不允许随便删除
  builtIn?: number
  // 后端自动维护的排序值
  sort?: number
  // 角色拥有的权限 id，新增角色和分配权限时传给后端
  permissionIds?: IdValue[]
}

// 权限表结构
export interface Permission {
  // 权限 id
  id?: IdValue
  // 权限编码，前后端都用它判断某个操作是否允许
  code: string
  // 权限名称，页面展示用
  name: string
  // 权限所属模块，比如用户管理、电子书管理
  module?: string
  // 权限对应的接口路径或页面路径
  path?: string
  // 权限显示顺序
  sort?: number
}

// 登录成功后保存在浏览器里的用户信息
export interface LoginUser {
  // 当前登录用户 id
  id: IdValue
  // 当前登录账号
  loginName: string
  // 当前登录用户姓名
  name: string
  // 登录 token，之后请求后端时会放到请求头里
  token: string
  // 当前用户角色 id 列表
  roleIds: IdValue[]
  // 当前用户角色名称列表
  roleNames: string[]
  // 当前用户角色编码列表
  roleCodes: string[]
  // 当前用户权限编码列表
  permissions: string[]
}

// 首页统计结构
export interface Statistic {
  // 总阅读数
  viewCount: number
  // 总点赞数
  voteCount: number
  // 今日阅读数
  todayViewCount: number
  // 今日点赞数
  todayVoteCount: number
}

// 最近 30 天统计结构
export interface StatisticRecord {
  // 统计日期
  date: string
  // 当天累计阅读数
  viewCount: number
  // 当天累计点赞数
  voteCount: number
  // 当天新增阅读数
  viewIncrease: number
  // 当天新增点赞数
  voteIncrease: number
}

// 小游戏排行榜结构
// 钢琴块、连连看、2048、消消乐、拼图都用这个结构展示排行榜
export interface GameLeaderboardItem {
  // 排名，从 1 开始
  rank: number
  // 成绩所属用户 id
  userId: IdValue
  // 成绩所属用户账号
  loginName: string
  // 成绩所属用户姓名
  userName: string
  // 游戏分数，具体含义由不同小游戏决定
  score: number
  // 本局总操作次数，比如按键次数、移动次数、步数
  totalPressCount: number
  // 本局错误次数
  wrongCount: number
  // 准确率，通常是 0 到 100
  accuracy: number
  // 最大连击数，适合钢琴块、消消乐这类游戏
  maxCombo: number
  // 结束原因，比如完成、超时、按错
  reason: string
  // 成绩创建时间
  createTime: string
}

// 钢琴块排行榜结构
// 旧名字继续保留，避免已经写好的钢琴块页面大面积改名
export type PianoLeaderboardItem = GameLeaderboardItem

// 收藏状态
export interface FavoriteStatus {
  // 当前登录用户是否已经收藏
  favorited: boolean
  // 当前收藏所在分组；未收藏时一般是默认分组
  folderName?: string
}

// 用户收藏分组结构
export interface UserFavoriteFolder {
  // 收藏分组 id
  id?: IdValue
  // 分组所属用户 id
  userId: IdValue
  // 分组名称
  name: string
  // 分组创建时间
  createTime?: string
}

// 用户收藏结构
export interface UserFavorite {
  // 收藏记录 id
  id?: IdValue
  // 收藏所属用户 id
  userId: IdValue
  // 收藏目标类型，ebook 表示电子书，doc 表示文档
  targetType: 'ebook' | 'doc'
  // 收藏目标 id
  targetId: IdValue
  // 收藏目标名称，后端返回给页面展示用
  targetName?: string
  // 收藏文档时所属电子书 id
  ebookId?: IdValue
  // 收藏分组名称
  folderName?: string
  // 收藏创建时间
  createTime?: string
}

// 阅读历史结构
export interface ReadingHistory {
  // 阅读历史 id
  id?: IdValue
  // 阅读人用户 id
  userId: IdValue
  // 阅读的电子书 id
  ebookId: IdValue
  // 阅读的文档 id
  docId: IdValue
  // 电子书名称，后端返回给页面展示用
  ebookName?: string
  // 文档名称，后端返回给页面展示用
  docName?: string
  // 最近阅读时间
  readTime?: string
  // 阅读进度百分比，范围 0 到 100
  progress?: number
}

// 评论结构
export interface UserComment {
  // 评论 id
  id?: IdValue
  // 评论人用户 id
  userId?: IdValue
  // 评论目标类型，ebook 表示电子书，doc 表示文档
  targetType: 'ebook' | 'doc'
  // 评论目标 id
  targetId: IdValue
  // 父评论 id，0 或空表示一级评论
  parentId?: IdValue
  // 评论正文
  content: string
  // 评论状态：已发布、待审核、已驳回、已删除
  status?: 'published' | 'pending' | 'rejected' | 'deleted'
  // 命中的敏感词，后端审核时返回
  sensitiveHit?: string
  // 评论创建时间
  createTime?: string
  // 审核时间
  reviewTime?: string
  // 审核人用户 id
  reviewUserId?: IdValue
  // 审核备注，驳回时说明原因
  reviewRemark?: string
  // 评论人姓名，页面展示用
  userName?: string
  // 评论目标名称，页面展示用
  targetName?: string
  // 前端评论树展示用的子评论列表
  children?: UserComment[]
}

// 用户反馈结构
export interface UserFeedback {
  // 反馈 id
  id?: IdValue
  // 提交反馈的用户 id
  userId?: IdValue
  // 反馈目标类型，ebook 表示电子书，doc 表示文档
  targetType?: 'ebook' | 'doc'
  // 反馈目标 id
  targetId?: IdValue
  // 反馈类型，suggestion 表示建议，correction 表示纠错
  type: 'suggestion' | 'correction'
  // 反馈标题
  title: string
  // 反馈内容
  content: string
  // 反馈状态：未处理、已处理、已驳回
  status?: 'open' | 'handled' | 'rejected'
  // 反馈创建时间
  createTime?: string
  // 管理员处理时间
  handleTime?: string
  // 处理人用户 id
  handleUserId?: IdValue
  // 处理备注，说明处理结果或驳回原因
  handleRemark?: string
  // 提交反馈的用户姓名
  userName?: string
}

// 反馈回复结构
export interface FeedbackReply {
  // 回复 id
  id?: IdValue
  // 所属反馈 id
  feedbackId: IdValue
  // 回复人用户 id
  userId?: IdValue
  // 回复类型，admin 表示管理员回复，user 表示普通用户回复
  replyType: 'admin' | 'user'
  // 回复内容
  content: string
  // 回复创建时间
  createTime?: string
  // 回复人姓名
  userName?: string
}

// 敏感词结构
export interface SensitiveWord {
  // 敏感词 id
  id?: IdValue
  // 敏感词内容
  word: string
  // 是否启用，1 表示启用，0 表示停用
  enabled: number
  // 备注说明
  remark?: string
  // 创建时间
  createTime?: string
}

// 用户通知结构
export interface UserNotice {
  // 通知 id
  id?: IdValue
  // 接收通知的用户 id
  userId: IdValue
  // 通知标题
  title: string
  // 通知内容
  content: string
  // 是否已读，1 表示已读，0 表示未读
  readFlag: number
  // 通知创建时间
  createTime?: string
}

// 操作日志结构
export interface OperationLog {
  // 操作日志 id
  id?: IdValue
  // 操作人用户 id
  userId?: IdValue
  // 操作模块，比如用户管理、角色权限、文档审核
  module: string
  // 操作动作，比如新增、修改、删除、审核
  action: string
  // 操作内容摘要
  content?: string
  // 操作前的关键数据
  beforeData?: string
  // 操作后的关键数据
  afterData?: string
  // 操作时间
  createTime?: string
}

// 文档历史版本结构
export interface DocVersion {
  // 版本记录 id
  id?: IdValue
  // 所属文档 id
  docId: IdValue
  // 版本号
  versionNo: number
  // 当前版本里的文档标题
  name?: string
  // 当前版本里的文档状态
  status?: string
  // 当前版本里的文档正文
  content?: string
  // 创建这个版本的用户 id
  createUserId?: IdValue
  // 版本创建时间
  createTime?: string
}

// 后台看板结构
export interface AdminDashboard {
  // 电子书总数
  ebookCount: number
  // 已发布电子书数量
  publishedEbookCount: number
  // 待审核电子书数量
  pendingEbookCount: number
  // 待审核文档数量
  pendingDocCount?: number
  // 文档总数
  docCount: number
  // 用户总数
  userCount: number
  // 待审核评论数量
  pendingCommentCount: number
  // 未处理反馈数量
  openFeedbackCount: number
  // 今日阅读数量
  todayReadCount: number
  // 阅读和点赞趋势图数据
  trend?: StatisticRecord[]
  // 待处理事项统计
  todoStats?: ChartItem[]
  // 电子书状态统计
  ebookStatusStats?: ChartItem[]
  // 分类阅读排行
  categoryRank?: ChartItem[]
}

// 看板图表通用结构
export interface ChartItem {
  // 图表项名称
  name: string
  // 图表项数值
  value: number
}
