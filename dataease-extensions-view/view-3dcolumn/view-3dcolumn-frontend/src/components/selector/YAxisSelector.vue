<template>
  <div style="width: 100%">
    <el-col>
      <el-form
        ref="axisForm"
        :model="axisForm"
        label-width="80px"
        size="mini"
      >
        <!-- 显示 -->
        <el-form-item
          :label="$t('chart.show')"
          class="form-item"
        >
          <el-checkbox
            v-model="axisForm.show"
            @change="changeYAxisStyle('show')"
          >{{ $t('chart.show') }}</el-checkbox>
        </el-form-item>
        <div v-show="axisForm.show">
          <!-- 位置 -->
          <el-form-item
            :label="$t('chart.position')"
            class="form-item"
          >
            <el-radio-group
              v-model="axisForm.position"
              size="mini"
              @change="changeYAxisStyle('position')"
            >
              <el-radio-button label="left">{{ $t('chart.text_pos_left') }}</el-radio-button>
              <el-radio-button label="right">{{ $t('chart.text_pos_right') }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <!-- 名称 -->
          <el-form-item
            :label="$t('chart.name')"
            class="form-item"
          >
            <el-input
              v-model="axisForm.name"
              size="mini"
              @blur="changeYAxisStyle('name')"
            />
          </el-form-item>
          <!-- 名称颜色 -->
          <el-form-item
            :label="$t('chart.axis_name_color')"
            class="form-item"
          >
            <el-color-picker
              v-model="axisForm.nameTextStyle.color"
              class="color-picker-style"
              :predefine="predefineColors"
              @change="changeYAxisStyle('nameTextStyle')"
            />
          </el-form-item>
          <!-- 名称字体 -->
          <el-form-item
            :label="$t('chart.axis_name_fontsize')"
            class="form-item"
          >
            <el-select
              v-model="axisForm.nameTextStyle.fontSize"
              :placeholder="$t('chart.axis_name_fontsize')"
              @change="changeYAxisStyle('nameTextStyle')"
            >
              <el-option
                v-for="option in fontSize"
                :key="option.value"
                :label="option.name"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <!-- softMax -->
          <el-form-item class="form-item form-item-slider">
            <span slot="label">
              <span class="span-box">
                <span>{{ $t('plugin_view_3d_bar.soft_max') }}</span>
                <el-tooltip
                  class="item"
                  effect="dark"
                  placement="bottom"
                >
                  <div slot="content">
                    {{ $t('plugin_view_3d_bar.soft_max_tooltip')}}
                  </div>
                  <i
                    class="el-icon-info"
                    style="cursor: pointer;"
                  />
                </el-tooltip>
              </span>
            </span>
            <el-slider
              v-model="axisForm.softMax"
              :min="0"
              :max="1000"
              show-input
              :show-input-controls="false"
              input-size="mini"
              @change="changeYAxisStyle('softMax')"
            />
          </el-form-item>
          <!-- softMin -->
          <el-form-item class="form-item form-item-slider">
            <span slot="label">
              <span class="span-box">
                <span>
                  {{ $t('plugin_view_3d_bar.soft_min') }}
                </span>
                <el-tooltip
                  class="item"
                  effect="dark"
                  placement="bottom"
                >
                  <div slot="content">
                    {{ $t('plugin_view_3d_bar.soft_min_tooltip')}}
                  </div>
                  <i
                    class="el-icon-info"
                    style="cursor: pointer;"
                  />
                </el-tooltip>
              </span>
            </span>
            <el-slider
              v-model="axisForm.softMin"
              :min="1"
              :max="10"
              show-input
              :show-input-controls="false"
              input-size="mini"
              @change="changeYAxisStyle('softMin')"
            />
          </el-form-item>

          <el-divider />

<!--          <span>-->
<!--            &lt;!&ndash; 刻度间距 &ndash;&gt;-->
<!--            <el-form-item-->
<!--              :label="$t('chart.axis_value_split')"-->
<!--              class="form-item"-->
<!--            >-->
<!--              <span slot="label">-->
<!--                <span class="span-box">-->
<!--                  <span>{{ $t('chart.axis_value_split_space') }}</span>-->
<!--                  <el-tooltip-->
<!--                    class="item"-->
<!--                    effect="dark"-->
<!--                    placement="bottom"-->
<!--                  >-->
<!--                    <div slot="content">-->
<!--                      间隔表示两个刻度之间的单位长度。-->
<!--                    </div>-->
<!--                    <i-->
<!--                      class="el-icon-info"-->
<!--                      style="cursor: pointer;"-->
<!--                    />-->
<!--                  </el-tooltip>-->
<!--                </span>-->
<!--              </span>-->
<!--              <el-input-->
<!--                v-model="axisForm.axisValue.split"-->
<!--                @blur="changeYAxisStyle('axisValue')"-->
<!--              />-->
<!--            </el-form-item>-->
<!--          </span>-->
<!--         -->
<!--          <el-divider/>-->

          <!-- 轴线显示 -->
<!--          <el-form-item-->
<!--            :label="$t('chart.axis_show')"-->
<!--            class="form-item"-->
<!--          >-->
<!--            <el-checkbox-->
<!--              v-model="axisForm.axisLine.show"-->
<!--              @change="changeYAxisStyle('axisLine')"-->
<!--            >{{ $t('chart.axis_show') }}</el-checkbox>-->
<!--          </el-form-item>-->
          <!-- 网格线显示 -->
          <el-form-item
            :label="$t('chart.grid_show')"
            class="form-item"
          >
            <el-checkbox
              v-model="axisForm.splitLine.show"
              @change="changeYAxisStyle('splitLine')"
            >{{ $t('chart.grid_show') }}</el-checkbox>
          </el-form-item>
          <!-- 网格线颜色 -->
          <span v-show="axisForm.splitLine.show">
            <el-form-item
              :label="$t('chart.grid_color')"
              class="form-item"
            >
              <el-color-picker
                v-model="axisForm.splitLine.lineStyle.color"
                class="el-color-picker"
                :predefine="predefineColors"
                @change="changeYAxisStyle('splitLine')"
              />
            </el-form-item>
            <!-- 网格线宽度 -->
            <el-form-item
              :label="$t('chart.grid_width')"
              class="form-item form-item-slider"
            >
              <el-slider
                v-model="axisForm.splitLine.lineStyle.width"
                :min="1"
                :max="10"
                show-input
                :show-input-controls="false"
                input-size="mini"
                @change="changeYAxisStyle('splitLine')"
              />
            </el-form-item>
            <!-- 网格线类型 -->
            <el-form-item
              :label="$t('chart.grid_type')"
              class="form-item"
            >
              <el-radio-group
                v-model="axisForm.splitLine.lineStyle.type"
                size="mini"
                @change="changeYAxisStyle('splitLine')"
              >
                <el-radio-button label="solid">{{ $t('chart.axis_type_solid') }}</el-radio-button>
                <el-radio-button label="dashed">{{ $t('chart.axis_type_dashed') }}</el-radio-button>
                <el-radio-button label="dotted">{{ $t('chart.axis_type_dotted') }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </span>
          <el-divider/>
          <!-- 标签显示 -->
          <el-form-item
            :label="$t('chart.axis_label_show')"
            class="form-item"
          >
            <el-checkbox
              v-model="axisForm.axisLabel.show"
              @change="changeYAxisStyle('axisLabel')"
            >{{ $t('chart.axis_label_show') }}</el-checkbox>
          </el-form-item>

          <span v-show="axisForm.axisLabel.show">
            <!-- 标签颜色 -->
            <el-form-item
              :label="$t('chart.axis_label_color')"
              class="form-item"
            >
              <el-color-picker
                v-model="axisForm.axisLabel.color"
                class="el-color-picker"
                :predefine="predefineColors"
                @change="changeYAxisStyle('axisLabel')"
              />
            </el-form-item>
            <!-- 标签角度 -->
            <el-form-item
              :label="$t('chart.axis_label_rotate')"
              class="form-item form-item-slider"
            >
              <el-slider
                v-model="axisForm.axisLabel.rotate"
                show-input
                :show-input-controls="false"
                :min="-90"
                :max="90"
                input-size="mini"
                @change="changeYAxisStyle('axisLabel')"
              />
            </el-form-item>
            <!-- 标签大小 -->
            <el-form-item
              :label="$t('chart.axis_label_fontsize')"
              class="form-item"
            >
              <el-select
                v-model="axisForm.axisLabel.fontSize"
                :placeholder="$t('chart.axis_label_fontsize')"
                @change="changeYAxisStyle('axisLabel')"
              >
                <el-option
                  v-for="option in fontSize"
                  :key="option.value"
                  :label="option.name"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>

<!--            <span v-show="chart.type && !chart.type.includes('horizontal')">-->
<!--              &lt;!&ndash; 格式类型 &ndash;&gt;-->
<!--              <el-form-item-->
<!--                :label="$t('chart.value_formatter_type')"-->
<!--                class="form-item"-->
<!--              >-->
<!--                <el-select-->
<!--                  v-model="axisForm.axisLabelFormatter.type"-->
<!--                  @change="changeYAxisStyle('axisLabelFormatter')"-->
<!--                >-->
<!--                  <el-option-->
<!--                    v-for="type in typeList"-->
<!--                    :key="type.value"-->
<!--                    :label="$t('chart.' + type.name)"-->
<!--                    :value="type.value"-->
<!--                  />-->
<!--                </el-select>-->
<!--              </el-form-item>-->
<!--              &lt;!&ndash; 小数位数 &ndash;&gt;-->
<!--              <el-form-item-->
<!--                v-show="axisForm.axisLabelFormatter.type !== 'auto'"-->
<!--                :label="$t('chart.value_formatter_decimal_count')"-->
<!--                class="form-item"-->
<!--              >-->
<!--                <el-input-number-->
<!--                  v-model="axisForm.axisLabelFormatter.decimalCount"-->
<!--                  :precision="0"-->
<!--                  :min="0"-->
<!--                  :max="10"-->
<!--                  size="mini"-->
<!--                  @change="changeYAxisStyle('axisLabelFormatter')"-->
<!--                />-->
<!--              </el-form-item>-->
<!--              &lt;!&ndash; 数量单位 &ndash;&gt;-->
<!--              <el-form-item-->
<!--                v-show="axisForm.axisLabelFormatter.type !== 'percent'"-->
<!--                :label="$t('chart.value_formatter_unit')"-->
<!--                class="form-item"-->
<!--              >-->
<!--                <el-select-->
<!--                  v-model="axisForm.axisLabelFormatter.unit"-->
<!--                  :placeholder="$t('chart.pls_select_field')"-->
<!--                  size="mini"-->
<!--                  @change="changeYAxisStyle('axisLabelFormatter')"-->
<!--                >-->
<!--                  <el-option-->
<!--                    v-for="item in unitList"-->
<!--                    :key="item.value"-->
<!--                    :label="$t('chart.' + item.name)"-->
<!--                    :value="item.value"-->
<!--                  />-->
<!--                </el-select>-->
<!--              </el-form-item>-->
<!--              &lt;!&ndash; 单位后缀 &ndash;&gt;-->
<!--              <el-form-item-->
<!--                :label="$t('chart.value_formatter_suffix')"-->
<!--                class="form-item"-->
<!--              >-->
<!--                <el-input-->
<!--                  v-model="axisForm.axisLabelFormatter.suffix"-->
<!--                  size="mini"-->
<!--                  clearable-->
<!--                  :placeholder="$t('commons.input_content')"-->
<!--                  @change="changeYAxisStyle('axisLabelFormatter')"-->
<!--                />-->
<!--              </el-form-item>-->
<!--              &lt;!&ndash; 千分符 &ndash;&gt;-->
<!--              <el-form-item-->
<!--                :label="$t('chart.value_formatter_thousand_separator')"-->
<!--                class="form-item"-->
<!--              >-->
<!--                <el-checkbox-->
<!--                  v-model="axisForm.axisLabelFormatter.thousandSeparator"-->
<!--                  @change="changeYAxisStyle('axisLabelFormatter')"-->
<!--                />-->
<!--              </el-form-item>-->
<!--            </span>-->
          </span>
        </div>
      </el-form>
    </el-col>
  </div>
</template>

<script>

import {COLOR_PANEL, DEFAULT_YAXIS_STYLE, formatterType, unitList} from '@/utils/map';

export default {
  name: 'YAxisSelector',
  props: {
    param: {
      type: Object,
      required: true
    },
    chart: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      axisForm: JSON.parse(JSON.stringify(DEFAULT_YAXIS_STYLE)),
      isSetting: false,
      fontSize: [],
      predefineColors: COLOR_PANEL,
      typeList: formatterType,
      unitList: unitList
    }
  },
  watch: {
    'chart': {
      handler: function() {
        this.initData()
      }
    }
  },
  mounted() {
    this.init()
    this.initData()
  },
  methods: {
    initData() {
      const chart = JSON.parse(JSON.stringify(this.chart))
      if (chart.customStyle) {
        let customStyle = null
        if (Object.prototype.toString.call(chart.customStyle) === '[object Object]') {
          customStyle = JSON.parse(JSON.stringify(chart.customStyle))
        } else {
          customStyle = JSON.parse(chart.customStyle)
        }
        if (customStyle.yAxis) {
          this.axisForm = customStyle.yAxis
          if (!this.axisForm.splitLine) {
            this.axisForm.splitLine = JSON.parse(JSON.stringify(DEFAULT_YAXIS_STYLE.splitLine))
          }
          if (!this.axisForm.nameTextStyle) {
            this.axisForm.nameTextStyle = JSON.parse(JSON.stringify(DEFAULT_YAXIS_STYLE.nameTextStyle))
          }
          if (!this.axisForm.axisValue) {
            this.axisForm.axisValue = JSON.parse(JSON.stringify(DEFAULT_YAXIS_STYLE.axisValue))
          }
          if (!this.axisForm.axisLabelFormatter) {
            this.axisForm.axisLabelFormatter = JSON.parse(JSON.stringify(DEFAULT_YAXIS_STYLE.axisLabelFormatter))
          }
          if (!this.axisForm.axisLine) {
            this.axisForm.axisLine = JSON.parse(JSON.stringify(DEFAULT_YAXIS_STYLE.axisLine))
          }
        }
      }
    },
    init() {
      const arr = []
      for (let i = 6; i <= 40; i = i + 2) {
        arr.push({
          name: i + '',
          value: i + ''
        })
      }
      this.fontSize = arr
    },
    changeYAxisStyle(modifyName) {
      if (!this.axisForm.show) {
        this.isSetting = false
      }
      this.axisForm['modifyName'] = modifyName
      this.$emit('onChangeYAxisForm', this.axisForm)
    }
  }
}
</script>

<style scoped>
  .el-divider--horizontal {
    margin: 10px 0
  }
.shape-item{
  padding: 6px;
  border: none;
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.form-item-slider ::v-deep .el-form-item__label{
  font-size: 12px;
  line-height: 38px;
}
.form-item ::v-deep .el-form-item__label{
  font-size: 12px;
}
.el-select-dropdown__item{
  padding: 0 20px;
}
  span{
    font-size: 12px
  }
  .el-form-item{
    margin-bottom: 6px;
  }

.switch-style{
  position: absolute;
  right: 10px;
  margin-top: -4px;
}
  .color-picker-style{
    cursor: pointer;
    z-index: 1003;
  }
</style>
