/** 检查两端共用的手机号和密码字段，返回对应的提示文案。 */
export function loginFieldErrors(phone: string, password: string) {
  return {
    phone: /^1[3-9]\d{9}$/.test(phone) ? '' : '请输入 11 位中国大陆手机号',
    password: password ? '' : '请输入密码',
  }
}
