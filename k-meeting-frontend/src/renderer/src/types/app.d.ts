import 'vue'
import type Request from '@/utils/Request'
import type Utils from '@/utils/Utils'
import type Verify from '@/utils/Verify'
import type Message from '@/utils/Message'
import { Api } from '@/utils/Api'
import { Alert, Confirm } from '@/utils/Confirm'

declare module 'vue' {
  interface ComponentCustomProperties {
    Request: typeof Request
    Api: typeof Api
    Utils: typeof Utils
    Verify: typeof Verify
    Message: typeof Message
    Confirm: typeof Confirm
    Alert: typeof Alert
    imageAccept: string
  }
}
