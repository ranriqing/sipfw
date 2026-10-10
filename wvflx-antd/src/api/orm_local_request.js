import {
  httpAction as httpActionLocal,
} from '@/api/local_request'

// GET
export function getAction(uri,params) {
  return httpAction(uri, params, 'get')
}

// POST
export function postAction(uri,params) {
  return httpAction(uri, params, 'post')
}

// PUT
export function putAction(uri,params) {
  return httpAction(uri, params, 'put')
}

// PATCH
export function patchAction(uri,params) {
  return httpAction(uri, params, 'patch')
}

// DELETE
export function deleteAction(uri,params) {
  return httpAction(uri, params, 'delete')
}

// $METHOD = {post | put | ...}
export function httpAction(uri, params, method) {
  method || {}
  params = params || {}
  let actionPromise =  httpActionLocal(uri, params, method)
  // 再封装(获取gmodel)
  return new Promise((resolve, reject) => {
    actionPromise.then((resp) =>{
      resolve(resp.gmodel)
    }).catch(error => {
      reject(error)
    })
  })
}


