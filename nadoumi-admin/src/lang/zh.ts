// Minimal zh-CN stub. English is the default; this exists so the locale switch works.
import en from './en'

const zh: typeof en = JSON.parse(JSON.stringify(en))
zh.common.signIn = '登录'
zh.common.signOut = '退出登录'
zh.login.subtitle = '内部运营控制台'
export default zh
