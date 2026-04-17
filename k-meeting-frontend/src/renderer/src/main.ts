import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createApp } from 'vue'
import App from './App.vue'
import router from '@/router'
import '@/assets/icon/iconfont.css'
import '@/assets/base.scss'

import Titlebar from '@/components/TitleBar.vue'
import Header from '@/components/Header.vue'
import NoData from '@/components/NoData.vue'
import Dialog from '@/components/Dialog.vue'
import Cover from '@/components/Cover.vue'
import Avatar from '@/components/Avatar.vue'

import Request from "@/utils/Request"
import { Api } from "@/utils/Api"
import Utils from "@/utils/utils"
import Verify from "@/utils/verify"
import Message from "@/utils/Message"
import { Confirm, Alert } from '@/utils/Confirm'
import * as Pinia from "pinia"


const app = createApp(App)
app.use(Pinia.createPinia())
app.use(ElementPlus)
app.use(router)

app.component("Header", Header)
app.component("Titlebar", Titlebar)
app.component("NoData", NoData)
app.component("Dialog", Dialog)
app.component("Cover", Cover)
app.component("Avatar", Avatar)

app.config.globalProperties.Request = Request;
app.config.globalProperties.Api = Api;
app.config.globalProperties.Utils = Utils;
app.config.globalProperties.Verify = Verify;
app.config.globalProperties.Message = Message;
app.config.globalProperties.Confirm = Confirm;
app.config.globalProperties.Alert = Alert;
app.config.globalProperties.imageAccept = ".jpg,.png,.gif,.bmp,.webp";

app.mount('#app')
