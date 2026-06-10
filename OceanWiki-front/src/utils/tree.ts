// 文件说明：这个工具文件负责把后端返回的平铺数组转成树形结构。
import type { IdValue } from '@/types'
import { Tool } from '@/utils/tool'

// 把普通数组转成树形数组
// 分类和文档都有 parent 字段，所以可以共用这个工具
export function arrayToTree<T extends { id?: IdValue; parent: IdValue; children?: T[] }>(
  list: T[],
  parent: IdValue = 0,
): T[] {
  // Tool.array2Tree 会给节点追加 children，所以这里先复制一份，避免直接修改原数组。
  return Tool.array2Tree(Tool.copy(list) || [], parent)
}

// 根据分类 id 查分类名称
export function findNameById<T extends { id?: IdValue; name: string; children?: T[] }>(
  list: T[],
  id?: IdValue,
): string {
  if (!id) {
    return '-'
  }

  for (const item of list) {
    if (String(item.id) === String(id)) {
      return item.name
    }

    const childName = findNameById(item.children || [], id)
    if (childName !== '-') {
      return childName
    }
  }

  return '-'
}
