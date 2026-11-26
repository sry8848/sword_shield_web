/** 调用应用路由表确认站内回跳目标，返回可交给 Vue Router 的地址。 */
export function resolveLoginRedirect(target: unknown, isRegistered: (path: string) => boolean): string | undefined {
  // URL 参数来自浏览器，先限制为可解析的本站路径。
  if (typeof target !== 'string' || !target.startsWith('/') || target.startsWith('//') || target.includes('\\'))
    return

  const base = 'https://redirect.invalid'
  try {
    if (new URL(target, base).origin !== base)
      return
  }
  catch {
    return
  }

  return isRegistered(target) ? target : undefined
}
