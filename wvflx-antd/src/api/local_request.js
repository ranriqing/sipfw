// GET
export function getAction(uri, params) {
  return httpAction(uri, params, 'get')
}

// POST
export function postAction(uri, params) {
  return httpAction(uri, params, 'post')
}

// PUT
export function putAction(uri, params) {
  return httpAction(uri, params, 'put')
}

// PATCH
export function patchAction(uri, params) {
  return httpAction(uri, params, 'patch')
}

// DELETE
export function deleteAction(uri, params) {
  return httpAction(uri, params, 'delete')
}

// $METHOD = {post | put | ...}
export function httpAction(uri, params, method) {
  method && true
  params = params || {}
  return new Promise((resolve, reject) => {
    // 名称前缀 /demo/update/1  =>  demo_update_1
    let stripPrefixUri = uri.startsWith('/')?uri.substring(1):uri
    let prefixName = stripPrefixUri.replace(/\//g, '_')
    let suffixName = params.status?`_${params.status}.js`:'_ok.js'
    // Json结果
    let jsFile = require('@/api/data/' + prefixName + suffixName)
    if(params.status && params.status!=='ok'){
      reject(jsFile.default)
    }else{
      resolve(jsFile.default)
    }
  })
}
