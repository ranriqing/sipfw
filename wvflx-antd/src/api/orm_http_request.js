import {
  getAction as getActionHttp,
  deleteAction as deleteActionHttp,
  putAction as putActionHttp,
  patchAction as patchActionHttp,
  postAction as postActionHttp
} from '@/api/http_request'
import notification from 'ant-design-vue/lib/notification';

// GET
export function getAction(uri, params) {
  return _wrapPromise(uri, params, getActionHttp(uri, params))
}

// POST
export function postAction(uri, params) {
  return _wrapPromise(uri, params, postActionHttp(uri, params))
}

// PUT
export function putAction(uri, params) {
  return _wrapPromise(uri, params, putActionHttp(uri, params))
}

// PATCH
export function patchAction(uri, params) {
  return _wrapPromise(uri, params, patchActionHttp(uri, params))
}

// DELETE
export function deleteAction(uri, params) {
  return _wrapPromise(uri, params, deleteActionHttp(uri, params))
}

// 工具方法
function _wrapPromise(uri, params, promise){
  // Promise再封装
  return new Promise((resolve, reject) => {
    promise.then(resp =>{
      if(uri.endsWith('/index')){
        resolve(resp.gmodel.entityList)
      }else{
        resolve(resp.gmodel)
      }
    }).catch(err => {
      if(!err.hcode){
        // 非hcode错误
        if(err.code === '102'){
          // GcsState.PARAM_ERROR
          notification.warn({
            message: `[${err.code}]${err.message}`,
            description: function(h){
              h && true
              let vNodes = []
              if(err.gmodel && err.gmodel.errors){
                err.gmodel.errors.forEach(error => {
                  vNodes.push(<div>{error.message}</div>)
                })
              }
              return (
                <div>
                  {vNodes}
                </div>
              )
            },
            duration: 4
          });
        }else{
          // !GcsState.PARAM_ERROR
          notification.warn({
            message: `[${err.code}]`,
            description: `${err.message}`,
            duration: 4
          });
        }
        // 非hcode错误
        reject(err)
      }else{
        // 为hcode错误
        reject(err)
      }
    })
  })
}







