let configObj = {
  optionMap: {
    geo: [
      {
        map: 'china',
        aspectScale: 1,
        zoom: 0.9,
        layoutCenter: ['50%', '50%'],
        layoutSize: '100%',
        show: true,
        roam: false,
        label: {
          show: false, // 各个省市县的名字
          color: '#fff'
        },
        /* regions: [{
          name: '青海省',
          itemStyle: {
            normal: {
              opacity: 1, 
              areaColor: 'rgb(200,139,68)', 
              borderColor: "#d97a51", 
              borderWidth: 3, 
            },
          }
        }], */
        itemStyle: {
          // 每块的样式
          areaColor: {
            type: 'linear',
            x: 1200,
            y: 0,
            x2: 0,
            y2: 0,
            colorStops: [
              {
                offset: 0,
                color: 'rgba(3,27,78,0.75)'
              },
              {
                offset: 1,
                color: 'rgba(58,149,253,0.75)'
              }
            ],
            global: true // 缺省为 false
          },
          borderColor: '#c0f3fb',
          borderWidth: 0.8
        },
        emphasis: {
          itemStyle: {
            show: false,
            color: '#fff',
            areaColor: 'rgba(0,254,233,0.6)'
          },
          label: {
            show: false,
            color: '#fff'
          }
        }
      },
    ],
    visualMap: {
      min: 0,
      max: 200,
      calculable: true,
      seriesIndex: [0],
      inRange: {
        color: ['#e29148', '#d94e5d']
      },
      textStyle: {
        color: '#333'
      }
    },
    tooltip: {
      trigger: 'item',
    },
    series: [
      {
        type: 'scatter',
        zlevel: 1,
        coordinateSystem: 'geo',
        emphasis: {
          label: {
            show: false,
            position: 'top',
            color: '#fff',
            /*formatter: function() {
              return '{b|{b}}'
            }*/
          }
        },
        label: {
          show: true,
          position: 'top',
          color: '#fff',
          fontSize: 12,
          fontWeight: 'bold',
          formatter: function (params) {
            return params.data.value[2];
          }
        },
        tooltip: {
          trigger: 'item',
          // backgroundColor: '#dbf1e1',
          formatter: function (params) {
            return `城市: ${params.data.caption}<br/> 数量: ${params.data.value[2]}`;
          }
        },
        data: [],
        symbol: 'circle',
        // symbolSize: [20, 10],
        symbolSize: function (data) {
          return [data[2]/8, data[2]/8]; // 根据数量设置点的大小
        },
        itemStyle: {
          color: {
            type: 'radial',  // 径向渐变更贴近圆形光照
            x: 0.5, y: 0.5,  // 圆心位置
            r: 0.8,           // 渐变半径
            colorStops: [{
              offset: 0, color: '#50a3ba'  // 中心高亮
            }, {
              offset: 1, color: '#FF8C00'  // 边缘暗部
            }]
          },
          shadowBlur: 12,
          shadowColor: 'rgba(255,140,0,0.6)', // 主阴影
          shadowOffsetX: 4,  // 向右偏移模拟右侧光
          shadowOffsetY: 4,  // 向下偏移增强投影
          emphasis: {         // 悬停时阴影加深
            shadowBlur: 15,
            shadowColor: 'rgba(255,140,0,0.8)',
            borderColor: '#FAFAEA',
            borderWidth: 2
          },
          opacity: 0.9,
        },
        effectType: 'ripple',
        showEffectOn: 'render', //emphasis移入显示动画，render一开始显示动画
        rippleEffect: {
          scale: 5,
          brushType: 'stroke'
        }
      },
    ]
  },
}

export default configObj