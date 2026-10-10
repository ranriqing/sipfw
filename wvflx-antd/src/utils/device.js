import enquireJs from 'enquire.js'

// EnquireScreen
const enquireScreen = function (callBack) {
  let tabletHandler = {
    match: function () {
      callBack && callBack(0)
    },
    unmatch: function () {
      callBack && callBack(-1)
    }
  }
  let mobileHandler = {
    match: () => {
      callBack && callBack(1)
    },
    // Toggle device bug
    /*unmatch: function () {
      callBack && callBack(0)
    }*/
  }
  enquireJs.register('screen and (max-width: 1087.99px)', tabletHandler)
  enquireJs.register('screen and (max-width: 767.99px)', mobileHandler)
}

export default enquireScreen