/// <reference types="vite/client" />
/// <reference path="./types/electron.d.ts" />

interface ImportMetaEnv {
  readonly VITE_DOMAIN?: string
  readonly VITE_WS?: string
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'

  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>
  export default component
}
