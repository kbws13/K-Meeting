const windowManage = {}

const saveWindow = (id, window) => {
  windowManage[id] = window
}

const getWindow = (id) => {
  return windowManage[id]
}

const delWindow = (id) => {
  delete windowManage[id]
}

const getWindowManage = () => {
  return windowManage
}

export { saveWindow, getWindow, delWindow, getWindowManage }
