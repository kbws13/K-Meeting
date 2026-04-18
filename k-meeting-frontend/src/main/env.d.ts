interface ImportMetaEnv {
  readonly VITE_DOMAIN?: string
  readonly VITE_WS_CHECK?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
