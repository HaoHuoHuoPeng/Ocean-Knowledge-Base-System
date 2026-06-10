// 文件说明：这个工具文件负责处理富文本 HTML 内容，避免常见转义问题影响展示。

// wangEditor 或手动粘贴内容时，&nbsp; 有时会被二次转义成 &amp;nbsp;
// 浏览器会把 &nbsp; 渲染成空格，但会把 &amp;nbsp; 显示成文字“&nbsp;”
export const normalizeEditorHtml = (html?: string) => {
  return (html || '').replace(/&amp;nbsp;/g, '&nbsp;')
}
