// 水印工具方法
let setWatermark = (str1, str2) => {
  let id = 'f59d5a5e00163e240c92'
  if (document.getElementById(id) !== null) {
    document.body.removeChild(document.getElementById(id))
  }
  // 水印图片
  let canvas = document.createElement('canvas')
  canvas.width = 150
  canvas.height = 80
  let content = canvas.getContext('2d')
  content.rotate(-20 * Math.PI / 180) // 水印旋转角度
  content.font = '15px Vedana'
  content.fillStyle = '#666666'
  content.textAlign = 'center'
  content.textBaseline = 'middle'
  content.fillText(str1, canvas.width / 2, canvas.height) // 水印在画布的位置x，y轴
  content.fillText(str2, canvas.width / 2, canvas.height + 22)
  // 平铺背景
  let div = document.createElement('div')
  div.id = id
  div.style.pointerEvents = 'none'
  div.style.top = '40px'
  div.style.left = '0px'
  div.style.opacity = '0.15'
  div.style.position = 'fixed'
  div.style.zIndex = '100000'
  div.style.width = document.documentElement.clientWidth + 'px'
  div.style.height = document.documentElement.clientHeight  + 'px'
  div.style.background = 'url(' + canvas.toDataURL('image/png') + ') left top repeat'
  document.body.appendChild(div)
  return id
}

let utilObj = {
  // 添加水印
  setWaterMark: (str1, str2) => {
    let id = setWatermark(str1, str2)
    if (document.getElementById(id) === null) {
      id = setWatermark(str1, str2)
    }
    return id
  },
  // 移除水印
  removeWatermark: () => {
    let id = 'f59d5a5e00163e240c92'
    if (document.getElementById(id) !== null) {
      document.body.removeChild(document.getElementById(id))
    }
  }
}

export default utilObj