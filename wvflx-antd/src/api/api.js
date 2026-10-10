import {
  getAction as getActionLocal,
  postAction as postActionLocal,
  putAction as putActionLocal,
  patchAction as patchActionLocal,
  deleteAction as deleteActionLocal,
} from '@/api/local_request'

import {
  getAction as getActionHttp,
  postAction as postActionHttp,
  putAction as putActionHttp,
  patchAction as patchActionHttp,
  deleteAction as deleteActionHttp,
  httpAction as httpActionHttp,
  downloadAction as downloadActionHttp,
} from '@/api/http_request'

// -------- 基础操作Api -------- //
// 前端静态文件
let settings = require('@/settings')
import axios from 'axios'
import request from '@/utils/request'
export const publicAsset = (filePath)=>{
  if(filePath.startsWith('/maps/')){
    // return axios.get('https://geo.datav.aliyun.com/areas_v3/bound/' + filePath.substring(6))
    return axios.get('/geo-datav/areas_v3/bound/' + filePath.substring(6))
  }else{
    return axios.get(settings.WEB_PATH + filePath)
  }
};

// 元信息模块
export const ormMetas = (params)=>postActionHttp('/orm/metas', params);
export const ormCopyView = (params)=>postActionHttp('/orm/copyView', params);
export const ormCopyComponent = (params)=>postActionHttp('/orm/copyComponent', params);
export const ormSeqOrders = (entityName, params)=>postActionHttp(`/orm/${entityName}/seqOrders`, params);

// 登录注册模块
export const login = (params)=>postActionHttp('/auth/login', params);
export const clogin = (params)=>postActionHttp('/auth/clogin', params); // Code登录
export const logout = (params)=>getActionHttp('/auth/logout', params);
export const sendSmsCode = (params)=>postActionLocal('/auth/sendSmsCode', params);
export const refreshToken = (params)=>getActionHttp('/auth/refreshToken', params);

// 个人资料模块
export const uploadAvatar = (params)=>formAction('/avatar/uploadAvatar', params);
export const currentUser = (params)=>getActionHttp('/profile/currentUser', params);
export const updateProfile = (params)=>postActionHttp('/profile/updateUser', params);
export const refreshCcAuthCode = (params)=>postActionHttp('/profile/refreshCcAuthCode', params);
export const unbindCcExten = (params)=>postActionHttp('/profile/unbindCcExten', params);
export const stateCcExten = (params)=>postActionHttp('/profile/stateCcExten', params);

// 其它操作模块
export const uploadFile = (params)=>formAction('/gcsStorage/uploadFile', params);
export const dcImport = (params)=>formAction('/dcDataset/dcImport', params);
export const chartData = (params)=>postActionLocal('/report/chartData', params);
export const reportFakeData = (params)=>postActionLocal('/report/fakeData', params);

// 内容管理模块
export const cntNodeUpload = (spaceId, parentId, file)=>{
  let formData = new FormData();
  formData.append('file', file);
  let url = `/cnt/node/upload?spaceId=${spaceId}` + (parentId ? `&parentId=${parentId}` : '');
  return request({ url, method: 'post', data: formData, headers: {'Content-Type': 'multipart/form-data'} });
};
export const cntNodeDownload = (id)=>request({ url: `/cnt/node/download/${id}`, method: 'get', responseType: 'blob' });
export const uploadResource = (file)=>{
  let formData = new FormData();
  formData.append('file', file);
  return request({ url: '/gcsStorage/uploadFile', method: 'post', data: formData, headers: {'Content-Type': 'multipart/form-data'} });
};
export const shareContent = (token)=>request({ url: `/share/${token}/content`, method: 'get', responseType: 'blob' });
export const cntBatchZip = (ids)=>request({ url: `/cnt/batch/zip?ids=${ids.join(',')}`, method: 'get', responseType: 'blob' });

// -------- 常用操作Api -------- //
export function getAction(uri,params) { return getActionHttp(uri, params) }
export function postAction(uri,params) { return postActionHttp(uri, params) }
export function putAction(uri,id,params) { return putActionHttp(_concatUri(uri, id), params) }
export function patchAction(uri,id,params) { return patchActionHttp(_concatUri(uri, id), params) }
export function deleteAction(uri,id,params) { return deleteActionHttp(_concatUri(uri, id), params) }
export function downloadAction(uri,params, timeout) { return downloadActionHttp(uri, params, timeout) }
export function httpAction(uri, params, method, config, timeout) { return httpActionHttp(uri, params, method, config, timeout) }
export function formAction(uri, params) { return httpActionHttp(uri, params, 'post', {
  contentType: false, processData: false, headers:{'Content-Type': 'application/x-www-form-urlencoded'}
})}

// -------- Demo使用Api -------- //
// Local调用
(uri,params)=>getActionLocal(uri,params);
(uri,params)=>postActionLocal(uri,params);
(uri,id,params)=>putActionLocal(_concatUri(uri, id),params);
(uri,id,params)=>patchActionLocal(_concatUri(uri, id),params);
(uri,id,params)=>deleteActionLocal(_concatUri(uri, id),params);

// Http调用
(uri,params)=>getActionHttp(uri,params);
(uri,params)=>postActionHttp(uri,params);
(uri,id,params)=>putActionHttp(_concatUri(uri, id),params);
(uri,id,params)=>patchActionHttp(_concatUri(uri, id),params);
(uri,id,params)=>deleteActionHttp(_concatUri(uri, id),params);

// -------- 工具方法部分 -------- //
function _concatUri(uri, id){
  if(!id){
    return uri
  }else if(uri.endsWith('/')){
    return `${uri}${id}`
  }else{
    return `${uri}/${id}`
  }
}
