// 文件说明：这个文件负责管理小游戏用到的图片资源，包含默认物种图和本地上传图片读取。

// 小游戏图片资源结构
// 默认图片来自课程里给的 OSS 地址，上传图片来自用户本地选择的文件
export interface GameAsset {
  id: string
  name: string
  url: string
  source: 'default' | 'upload'
}

// 默认物种图片
// 连连看和消消乐都从这里取默认素材，后面要加物种图时只改这一处
export const defaultGameAssets: GameAsset[] = [
  {
    id: 'orca',
    name: '虎鲸',
    url: 'http://oceanwiki.oss-cn-guangzhou.aliyuncs.com/image/hujing.jpg',
    source: 'default',
  },
  {
    id: 'dolphin',
    name: '海豚',
    url: 'http://oceanwiki.oss-cn-guangzhou.aliyuncs.com/image/haitun.jpg',
    source: 'default',
  },
  {
    id: 'jellyfish',
    name: '水母',
    url: 'http://oceanwiki.oss-cn-guangzhou.aliyuncs.com/image/shuimu.jpg',
    source: 'default',
  },
  {
    id: 'seaweed',
    name: '海藻',
    url: 'http://oceanwiki.oss-cn-guangzhou.aliyuncs.com/image/haizao.jpg',
    source: 'default',
  },
  {
    id: 'sperm-whale',
    name: '抹香鲸',
    url: 'http://oceanwiki.oss-cn-guangzhou.aliyuncs.com/image/moxiangjing.jpg',
    source: 'default',
  },
]

// 把本地图片文件读取成 dataURL
// 浏览器不能直接把本地路径交给 img 使用，所以这里用 FileReader 转成页面可展示的地址
const readFileAsDataUrl = (file: File) => {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('图片读取失败'))
    reader.readAsDataURL(file)
  })
}

// 读取用户上传的多张图片
// startIndex 用来保证每张上传图的 id 不重复
export const createUploadedGameAssets = async (files: FileList, startIndex: number) => {
  const imageFiles = Array.from(files).filter((file) => file.type.startsWith('image/'))
  const assets: GameAsset[] = []

  for (const [index, file] of imageFiles.entries()) {
    const url = await readFileAsDataUrl(file)
    assets.push({
      id: `upload-${Date.now()}-${startIndex + index}`,
      name: file.name.replace(/\.[^.]+$/, '') || `上传图片${startIndex + index + 1}`,
      url,
      source: 'upload',
    })
  }

  return assets
}

// 按 id 找图片资源
// 游戏棋盘里只保存 id，真正显示时再通过这个方法拿到图片地址
export const findGameAsset = (assets: GameAsset[], id: string | null) => {
  if (!id) {
    return undefined
  }
  return assets.find((asset) => asset.id === id)
}
