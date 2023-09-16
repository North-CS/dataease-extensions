export const DEFAULT_COLOR_CASE = {
  value: 'default',
  colors: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'],
  alpha: 100,
  tableHeaderBgColor: '#e1eaff',
  tableItemBgColor: '#ffffff',
  tableFontColor: '#000000',
  tableStripe: true,
  dimensionColor: '#000000',
  quotaColor: '#000000',
  tableBorderColor: '#cfdaf4'
}


export const DEFAULT_SIZE = {
  barDefault: true,
  barWidth: 40,
  barGap: 0.4,
  lineWidth: 2,
  lineType: 'solid',
  lineSymbol: 'circle',
  lineSymbolSize: 4,
  lineSmooth: true,
  lineArea: false,
  pieInnerRadius: 0,
  pieOuterRadius: 60,
  pieRoseType: 'radius',
  pieRoseRadius: 5,
  funnelWidth: 80,
  radarShape: 'polygon',
  radarSize: 80,
  tableTitleFontSize: 12,
  tableItemFontSize: 12,
  tableTitleHeight: 36,
  tableItemHeight: 36,
  tablePageSize: '20',
  tableColumnMode: 'custom',
  tableColumnWidth: 100,
  tableHeaderAlign: 'left',
  tableItemAlign: 'right',
  tableAutoBreakLine: false,
  gaugeMinType: 'fix', // fix or dynamic
  gaugeMinField: {
    id: '',
    summary: ''
  },
  gaugeMin: 0,
  gaugeMaxType: 'fix', // fix or dynamic
  gaugeMaxField: {
    id: '',
    summary: ''
  },
  gaugeMax: 100,
  gaugeStartAngle: 225,
  gaugeEndAngle: -45,
  gaugeTickCount: 5,
  dimensionFontSize: 18,
  quotaFontSize: 18,
  spaceSplit: 10,
  dimensionShow: true,
  quotaShow: true,
  quotaFontFamily: 'Microsoft YaHei',
  quotaFontIsBolder: false,
  quotaFontIsItalic: false,
  quotaLetterSpace: '0',
  quotaFontShadow: false,
  dimensionFontFamily: 'Microsoft YaHei',
  dimensionFontIsBolder: false,
  dimensionFontIsItalic: false,
  dimensionLetterSpace: '0',
  dimensionFontShadow: false,
  scatterSymbol: 'circle',
  scatterSymbolSize: 20,
  treemapWidth: 80,
  treemapHeight: 80,
  liquidMax: 100,
  liquidMaxType: 'fix', // fix or dynamic
  liquidMaxField: {
    id: '',
    summary: ''
  },
  liquidSize: 80,
  liquidOutlineBorder: 4,
  liquidOutlineDistance: 8,
  liquidWaveLength: 128,
  liquidWaveCount: 3,
  liquidShape: 'circle',
  tablePageMode: 'page',
  symbolOpacity: 0.7,
  symbolStrokeWidth: 2,
  showIndex: false,
  indexLabel: '序号',
  hPosition: 'center',
  vPosition: 'center',
  mapPitch: 0,
  mapLineType: 'arc',
  mapLineWidth: 1,
  mapLineAnimate: true,
  mapLineAnimateDuration: 3,
  mapLineAnimateInterval: 1,
  mapLineAnimateTrailLength: 1,
  innerAngle: 15,
  outerAngle: 15,
  depth: 60,
  columnDepth: 100,
}

export const COLOR_PANEL = [
  '#ff4500',
  '#ff8c00',
  '#ffd700',
  '#90ee90',
  '#00ced1',
  '#1e90ff',
  '#c71585',
  '#999999',
  '#000000',
  '#FFFFFF'
]

export const DEFAULT_LABEL = {
  show: false,
  position: 'top',
  color: '#909399',
  fontSize: '10',
  formatter: '{c}',
  gaugeFormatter: '{value}',
  labelLine: {
    show: true
  }
}
export const DEFAULT_TOOLTIP = {
  show: true,
  trigger: 'item',
  confine: true,
  textStyle: {
    fontSize: '10',
    color: '#909399'
  },
  formatter: ''
}
export const DEFAULT_TITLE_STYLE = {
  show: true,
  fontSize: '18',
  color: '#303133',
  hPosition: 'center',
  vPosition: 'top',
  isItalic: false,
  isBolder: false
}
export const DEFAULT_BACKGROUND_COLOR = {
  color: '#ffffff',
  alpha: 100,
  borderRadius: 5
}
export const BASE_ECHARTS_SELECT = {
  itemStyle: {
    shadowBlur: 2
  }
}
export const BASE_PIE = {
  chart: {
    renderTo: 'container',
    type: 'column',
    options3d: {
      enabled: true,
      alpha: 15,
      beta: 15,
      depth: 50,
      viewDistance: 25
    }
  },
  title: {
    text: '',
    align: 'center',
    style: {
      fontWeight: 'normal'
    }
  },
  subtitle: {
    text: ''
  },
  legend: {
    enabled: false,
  },
  plotOptions: {
    column: {
      depth: 25
    },
  },
  tooltip: {
    backgroundColor: '',   // 背景颜色
    borderColor: '',         // 边框颜色
    style: {
      color: "",
      fontSize: ""
    },
    headerFormat: "",
    pointFormat: '{series.name}: <b>{point.y}</b>',
    enabled: false,
  },
  series: [{
    name: '',
    data: []
  }],
  xAxis: {
    data: [],
    title: {
      text: '',
      enabled: false,
      style: {},
    },
    labels: {
      enabled: true,
      rotation: 0,
      style: {}
    },
    // 间隔
    tickInterval: 1,
    // 网格线宽度
    gridLineWidth: 1,
    // 网格线颜色
    gridLineColor: '#C0C0C0',
    // 网格线条样式 Solid、Dot、Dash
    gridLineDashStyle: 'Dot',
    tickWidth: 0,
    tickLength: 6,
  },
  yAxis: {
    title: {
      text: '',
      enabled: false,
      style: {},
    },
    labels: {
      enabled: true,
      rotation: 0,
      style: {}
    },
    // 间隔
    tickInterval: 1,
    softMax: 100,
    softMin: 10,
    // 网格线宽度
    gridLineWidth: 1,
    // 网格线颜色
    gridLineColor: '#C0C0C0',
    opposite: true,
  },
  credits: {
    enabled: false //去掉Highcharts.com地址
  },
};


let terminalType = 'pc'
export function basePieOption(chart_option, chart, terminal = 'pc') {
  // console.log('chart:', chart);
  terminalType = terminal
  let customAttr = {}
  if (chart.customAttr) {
    customAttr = JSON.parse(chart.customAttr)
    // console.log('customAttr:', customAttr);
    if (customAttr.color) {
      chart_option.colors = customAttr.color.colors
    }

    // tooltip
    if (customAttr.tooltip) {
      const tooltip = JSON.parse(JSON.stringify(customAttr.tooltip));
      const reg = new RegExp('\n', 'g')
      tooltip.formatter = tooltip.formatter.replace(reg, '<br/>')

      chart_option.tooltip.enabled = tooltip.show;
      chart_option.tooltip.backgroundColor = tooltip.backgroundColor;
      chart_option.tooltip.borderColor = tooltip.backgroundColor;
      chart_option.tooltip.style = {fontSize: tooltip.textStyle.fontSize, color: tooltip.textStyle.color};
      chart_option.tooltip.headerFormat = "<small style=\"fontSize:" + tooltip.textStyle.fontSize + "\">{point.key}</small></br>";

      let formatter =  tooltip.formatter
      formatter = formatter.replace('{a}', '{series.name}')
      formatter = formatter.replace('{b}', '{point.name}')
      formatter = formatter.replace('{c}', '{point.y}')
      formatter = formatter.replace('{d', '{point.percentage')
      chart_option.tooltip.pointFormat = formatter
    }

    if (customAttr.size) {
      chart_option.chart.options3d.alpha = customAttr.size.innerAngle;
      chart_option.chart.options3d.beta = customAttr.size.outerAngle;
      chart_option.chart.options3d.depth = customAttr.size.depth;
      chart_option.plotOptions.column.depth = customAttr.size.columnDepth;
    }

    // label
    if (customAttr.label) {
        const dataLabels = {};
        dataLabels.enabled =  customAttr.label.show;
        dataLabels.color = customAttr.label.color;
        dataLabels.style = {color: customAttr.label.color, fontSize: customAttr.label.fontSize};
        const reg = new RegExp('\n', 'g');
        let formatter =  customAttr.label.formatter.replace(reg, '<br/>');
        formatter = formatter.replace('{a}', '{series.name}');
        formatter = formatter.replace('{b}', '{point.name}');
        formatter = formatter.replace('{c}', '{point.y}');
        formatter = formatter.replace('{d', '{point.percentage');
        dataLabels.format = formatter;

        chart_option.plotOptions.column.dataLabels = dataLabels;
      }
  }

  // 处理data
  if (chart.data) {
    chart_option.title.text = chart.title
    chart_option.xAxis.data = chart.data.x
    if (chart.data.series.length > 0) {
      chart_option.series[0].name = chart.data.series[0].name
      // size
      /*if (customAttr.size) {
        chart_option.series[0].radius = [customAttr.size.pieInnerRadius + '%', customAttr.size.pieOuterRadius + '%']
      }*/

      const valueArr = chart.data.series[0].data
      for (let i = 0; i < valueArr.length; i++) {
        const y = valueArr[i]
        y.name = chart.data.x[i]
        y.y = y.value
        chart_option.series[0].data.push(y)
      }
    }
  }

  componentStyle(chart_option, chart);
  return chart_option
}
export function componentStyle(chart_option, chart) {
  const padding = '8px';
  if (chart.customStyle) {
    const customStyle = JSON.parse(chart.customStyle)
    if (customStyle.text) {
      chart_option.title.text = customStyle.text.show ? chart.title : ''
      const style = chart_option.title.style ? chart_option.title.style : {}
      style.fontSize = customStyle.text.fontSize
      style.color = customStyle.text.color
      customStyle.text.isItalic ? style.fontStyle = 'italic' : style.fontStyle = 'normal'
      customStyle.text.isBolder ? style.fontWeight = 'bold' : style.fontWeight = 'normal'
      chart_option.title.textStyle = style
      chart_option.title.align = customStyle.text.hPosition
      chart_option.title.verticalAlign = customStyle.text.vPosition
    }

    if (customStyle.legend && chart_option.legend) {
      // chart_option.plotOptions.pie.showInLegend = customStyle.legend.show
      // chart_option.legend.padding = padding
      chart_option.legend.enabled = customStyle.legend.show;
      chart_option.legend.layout = customStyle.legend.orient
      chart_option.legend.verticalAlign = customStyle.legend.vPosition
      chart_option.legend.align = customStyle.legend.hPosition

      chart_option.legend.itemStyle = customStyle.legend.textStyle

    }

    if (customStyle.background) {
      chart_option.chart.backgroundColor = hexColorToRGBA(customStyle.background.color, customStyle.background.alpha)
    }

    // x 轴
    if (customStyle.xAxis) {
      chart_option.xAxis.title.enabled = customStyle.xAxis.show;
      chart_option.xAxis.title.text = customStyle.xAxis.name;
      const style = chart_option.xAxis.title.style ? chart_option.xAxis.title.style : {}
      style.fontSize = customStyle.xAxis.nameTextStyle.fontSize
      style.color = customStyle.xAxis.nameTextStyle.color
      chart_option.xAxis.title.style = style;

      // 间距
      chart_option.xAxis.tickInterval = customStyle.xAxis.split;

      // 轴线显示
      chart_option.xAxis.tickWidth = customStyle.xAxis.axisLine.show ? 1 : 0;

      // 网格线
      chart_option.xAxis.gridLineWidth = customStyle.xAxis.show ? (customStyle.xAxis.splitLine.show ? customStyle.xAxis.splitLine.lineStyle.width : 0) : 0;
      chart_option.xAxis.gridLineColor = customStyle.xAxis.splitLine.lineStyle.color;
      let gridLineDashStyle = "Solid";
      if (customStyle.xAxis.splitLine.lineStyle.type) {
        if (customStyle.xAxis.splitLine.lineStyle.type === 'dashed') {
          gridLineDashStyle = 'Dash';
        } else if (customStyle.xAxis.splitLine.lineStyle.type === 'dotted') {
          gridLineDashStyle = 'Dot';
        }
      }
      chart_option.xAxis.gridLineDashStyle = customStyle.xAxis.splitLine.lineStyle.type ? gridLineDashStyle : customStyle.xAxis.splitLine.lineStyle.style;

      // 标签
      chart_option.xAxis.labels.enabled = customStyle.xAxis.show ? customStyle.xAxis.axisLabel.show : false;
      chart_option.xAxis.labels.style.color = customStyle.xAxis.axisLabel.color;
      chart_option.xAxis.labels.style.fontSize = customStyle.xAxis.axisLabel.fontSize;
      chart_option.xAxis.labels.rotation = customStyle.xAxis.axisLabel.rotate;
      // customStyle.labels.formatter = function () {
      //   return this.value;
      // };
    }

    // y 轴
    if (customStyle.yAxis) {
      chart_option.yAxis.title.enabled = customStyle.yAxis.show;
      chart_option.yAxis.opposite = customStyle.yAxis.position === 'left' ? false : true;
      chart_option.yAxis.title.text = customStyle.yAxis.name;
      const style = chart_option.yAxis.title.style ? chart_option.yAxis.title.style : {}
      style.fontSize = customStyle.yAxis.nameTextStyle.fontSize
      style.color = customStyle.yAxis.nameTextStyle.color
      chart_option.yAxis.title.style = style;

      // 间距
      chart_option.yAxis.tickInterval = customStyle.yAxis.split;

      // 网格线
      chart_option.yAxis.gridLineWidth = customStyle.yAxis.show ? (customStyle.yAxis.splitLine.show ? customStyle.yAxis.splitLine.lineStyle.width : 0) : 0;
      chart_option.yAxis.gridLineColor = customStyle.yAxis.splitLine.lineStyle.color;
      let gridLineDashStyle = "Solid";
      if (customStyle.yAxis.splitLine.lineStyle.type) {
        if (customStyle.yAxis.splitLine.lineStyle.type === 'dashed') {
          gridLineDashStyle = 'Dash';
        } else if (customStyle.yAxis.splitLine.lineStyle.type === 'dotted') {
          gridLineDashStyle = 'Dot';
        }
      }
      chart_option.yAxis.gridLineDashStyle = customStyle.yAxis.splitLine.lineStyle.type ? gridLineDashStyle : customStyle.yAxis.splitLine.lineStyle.style;

      chart_option.yAxis.labels.enabled = customStyle.yAxis.show ? customStyle.yAxis.axisLabel.show : false;
      chart_option.yAxis.labels.style.color = customStyle.yAxis.axisLabel.color;
      chart_option.yAxis.labels.style.fontSize = customStyle.yAxis.axisLabel.fontSize;
      chart_option.yAxis.labels.rotation = customStyle.yAxis.axisLabel.rotate;

      // 标签格式
      // customStyle.labels.formatter = function () {
      //   return this.value;
      // };

    }
  }
}
export function hexColorToRGBA(hex, alpha) {
  const rgb = [] // 定义rgb数组
  if (/^\#[0-9A-F]{3}$/i.test(hex)) { // 判断传入是否为#三位十六进制数
    let sixHex = '#'
    hex.replace(/[0-9A-F]/ig, function(kw) {
      sixHex += kw + kw // 把三位16进制数转化为六位
    })
    hex = sixHex // 保存回hex
  }
  if (/^#[0-9A-F]{6}$/i.test(hex)) { // 判断传入是否为#六位十六进制数
    hex.replace(/[0-9A-F]{2}/ig, function(kw) {
      // eslint-disable-next-line no-eval
      rgb.push(eval('0x' + kw)) // 十六进制转化为十进制并存如数组
    })
    return `rgba(${rgb.join(',')},${alpha / 100})` // 输出RGB格式颜色
  } else {
    return 'rgb(0,0,0)'
  }
}



export const DEFAULT_YAXIS_EXT_STYLE = {
  show: true,
  position: 'right',
  name: '',
  nameTextStyle: {
    color: '#333333',
    fontSize: 12
  },
  axisLabel: {
    show: true,
    color: '#333333',
    fontSize: '12',
    rotate: 0,
    formatter: '{value}'
  },
  splitLine: {
    show: true,
    lineStyle: {
      color: '#cccccc',
      width: 1,
      style: 'solid'
    }
  },
  axisValue: {
    auto: true,
    min: null,
    max: null,
    split: null,
    splitCount: null
  }
}

export function uuid() {
  return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
}

