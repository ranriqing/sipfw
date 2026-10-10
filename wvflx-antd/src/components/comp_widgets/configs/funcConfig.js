// 判断当前经纬度是否在规定区域内
function isPointInMultiPolygon(point, multiPolygons) {
  for (let polygon of multiPolygons) {
    let coordinates = polygon.geometry.coordinates
    if (isPointInAnyPolygon(point, coordinates)) {
      return true
    }
  }
  return false
}

function isPointInAnyPolygon(point, polygons) {
  for (let polygon of polygons) {
    if (isPointInPolygon(point, polygon[0])) {
      return true
    }
  }
  return false
}

function isPointInPolygon(point, polygon) {
  // 射线法判断点是否在多边形内部
  let x = point[0],
    y = point[1]
  let inside = false
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    let xi = polygon[i][0],
      yi = polygon[i][1]
    let xj = polygon[j][0],
      yj = polygon[j][1]
    let intersect = yi > y !== yj > y && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi
    if (intersect) {
      inside = !inside
    }
  }
  return inside
}

function formatJson(chinaGeoJson) {
  chinaGeoJson.features.forEach((v) => {
    if (v.properties && v.properties.name == '海南省') {
      v.geometry.coordinates = v.geometry.coordinates.slice(0, 1)
    }
  })
  // 过滤掉海南诸岛边界线
  chinaGeoJson.features = chinaGeoJson.features.filter((item) => item.properties.adcode !== '100000_JD')
  return chinaGeoJson
}

let configObj = {
  // 南沙诸岛以缩略图展示
  formatJson: formatJson,
  // 判断当前经纬度是否在规定区域内
  isPointInMultiPolygon: isPointInMultiPolygon,
}

export default configObj