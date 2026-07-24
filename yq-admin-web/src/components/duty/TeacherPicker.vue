<template>
  <el-select
    :value="value"
    class="teacher-picker"
    filterable
    clearable
    placeholder="选择值班老师"
    @input="$emit('input', $event)"
  >
    <el-option
      v-for="teacher in teachers"
      :key="teacher.id"
      :label="teacher.name"
      :value="teacher.id"
    >
      <span class="teacher-option-name">{{ teacher.name }}</span>
      <small v-if="isBusy(teacher.id)" class="teacher-busy">当日有课</small>
    </el-option>
  </el-select>
</template>

<script>
export default {
  name: 'TeacherPicker',
  props: {
    value: { type: Number, default: null },
    teachers: { type: Array, default: () => [] },
    busyTeacherIds: { type: Array, default: () => [] }
  },
  methods: {
    isBusy(id) {
      return this.busyTeacherIds.includes(id)
    }
  }
}
</script>
