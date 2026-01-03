/**
 * 全局类型声明。
 *
 * - 引入 Vite 的客户端类型
 * - 声明 `.vue` 单文件组件的模块类型（便于 TS 正确推断）
 */
/// <reference types="vite/client" />
declare module '*.vue' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
