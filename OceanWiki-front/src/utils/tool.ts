// 文件说明：这个工具类来自教案参考文件，提供空值判断、对象复制、数组转树和前端临时 key 生成等通用方法。
export class Tool {
  // 空校验，null、undefined、空字符串、空数组、空对象都返回 true。
  public static isEmpty(obj: any) {
    if (typeof obj === 'string') {
      return !obj || obj.replace(/\s+/g, '') === ''
    }
    return !obj || JSON.stringify(obj) === '{}' || obj.length === 0
  }

  // 非空校验。
  public static isNotEmpty(obj: any) {
    return !this.isEmpty(obj)
  }

  // 对象复制。
  // 分类树转换会给节点追加 children，这里先复制一份，避免直接改后端返回的原始数组。
  public static copy<T>(obj: T): T | undefined {
    if (Tool.isNotEmpty(obj)) {
      return JSON.parse(JSON.stringify(obj))
    }
    return undefined
  }

  // 使用递归将数组转为树形结构。
  // 父 ID 属性固定为 parent，分类和文档目录都可以使用这个方法。
  public static array2Tree<T extends { id?: any; parent: any; children?: T[] }>(array: T[] | undefined, parentId: any): T[] {
    if (Tool.isEmpty(array)) {
      return []
    }

    const result: T[] = []
    for (let i = 0; i < array!.length; i++) {
      const item = array![i]
      if (String(item.parent) === String(parentId)) {
        result.push(item)

        const children = Tool.array2Tree(array, item.id)
        if (Tool.isNotEmpty(children)) {
          item.children = children
        }
      }
    }
    return result
  }

  // 生成前端临时 key。
  // 注意：项目里的业务 ID 统一由后端 MyBatis-Plus 雪花算法生成，前端不能用这个方法生成数据库主键。
  public static tempKey(len = 12, radix = 62) {
    const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'.split('')
    const key = []
    radix = radix || chars.length

    for (let i = 0; i < len; i++) {
      key[i] = chars[0 | (Math.random() * radix)]
    }

    return key.join('')
  }

  // 兼容教案里的旧写法。实际使用的是雪花id
  // 已不建议使用这个名字，避免误以为它能生成系统业务 ID。
  public static uuid(len = 12, radix = 62) {
    return Tool.tempKey(len, radix)
  }
}
