/** 精简打包依赖 */
const settings = require('@/settings')
import axios from 'axios'
import { postAction as postActionLocal } from '@/api/local_request'

/** 定义Axios */
const axiosService = axios.create({
  baseURL: settings.BASE_URL,
  timeout: 15000,
})
axiosService.interceptors.response.use(resp => {
  let respData = resp.data
  if (resp.data instanceof Blob) {          // 返回Blob数据
    return respData
  } if (respData.code === '0') {            // 返回成功Code(SUCCESS)
    return respData
  }else{
    console.log(`Code failed[${respData.code}]`, JSON.stringify(respData))
    return Promise.reject(respData)
  }
})

export const login = (params)=> axiosService({
  url: '/auth/login',
  method:'post' ,
  data: params
});
export const sendSmsCode = (params)=>postActionLocal('/auth/sendSmsCode', params);

/** 定义请求方法 */
export function getAction(uri,params) {
  return axiosService({
    url: uri,
    method:'get' ,
    params: params
  })
}

export const postAction = (uri, params)=>axiosService({
  url: uri,
  method:'post' ,
  data: params
});

export const putAction = (uri, params)=>axiosService({
  url: uri,
  method:'put' ,
  data: params
});

export const patchAction = (uri, params)=>axiosService({
  url: uri,
  method:'put' ,
  data: params
});

export const deleteAction = (uri, params)=>axiosService({
  url: uri,
  method:'delete' ,
  params: params
});
