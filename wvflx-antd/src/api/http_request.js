import axios from '@/utils/request'

// GET
export function getAction(uri, params) {
  return axios({
    url: uri,
    method: 'get',
    params: params
  })
}

// POST
export function postAction(uri, params) {
  return axios({
    url: uri,
    method:'post' ,
    data: params
  })
}

// PUT
export function putAction(uri, params) {
  return axios({
    url: uri,
    method:'put',
    data: params
  })
}

// PATCH
export function patchAction(uri, params) {
  return axios({
    url: uri,
    method:'patch',
    data: params
  })
}

// DELETE
export function deleteAction(uri, params) {
  return axios({
    url: uri,
    method: 'delete',
    params: params
  })
}

// DOWNLOAD
export function downloadAction(uri, params, timeout){
  return axios({
    url: uri,
    method:'post' ,
    data: params,
    responseType: 'blob',
    timeout: timeout || 15000,
  })
}

// $METHOD = {post | put | ...}
export function httpAction(uri, params, method, config, timeout){
  return axios({
    url: uri,
    method: method ,
    data: params,
    config: config,
    timeout: timeout || 15000,
  })
}

