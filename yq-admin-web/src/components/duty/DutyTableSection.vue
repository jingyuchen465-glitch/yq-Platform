<template>
  <section class="duty-section class-duty-section">
    <div class="section-heading">
      <div class="section-index">{{ time }}</div>
      <div>
        <h2>{{ title }}</h2>
        <p>{{ subtitle }}</p>
      </div>
      <span class="type-tag" :class="tone + '-tag'">{{ code }}</span>
    </div>
    <div v-if="rows.length" class="duty-table-wrap">
      <el-table :data="rows" stripe>
        <el-table-column label="班级" min-width="190">
          <template slot-scope="scope">
            <div class="class-cell">
              <i class="el-icon-school"></i>
              <b>{{ scope.row.className }}</b>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="教学周期" min-width="220">
          <template slot-scope="scope">
            <span class="period-text">{{ scope.row.teachingStartDate }} 至 {{ scope.row.teachingEndDate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="值班老师" min-width="230">
          <template slot-scope="scope">
            <teacher-picker
              v-model="scope.row.teacherId"
              :teachers="teachers"
              :busy-teacher-ids="busyTeacherIds"
            />
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="220">
          <template slot-scope="scope">
            <el-input v-model="scope.row.remark" maxlength="255" placeholder="备注（选填）" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="132" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" :loading="scope.row.saving" @click="$emit('save', scope.row)">保存</el-button>
            <el-button
              type="text"
              class="danger-link"
              :disabled="!scope.row.assignmentId"
              @click="$emit('clear', scope.row)"
            >清空</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-empty v-else description="当前日期没有教学周期内的班级" :image-size="72" />
  </section>
</template>

<script>
import TeacherPicker from '@/components/duty/TeacherPicker.vue'

export default {
  name: 'DutyTableSection',
  components: { TeacherPicker },
  props: {
    title: String,
    subtitle: String,
    time: String,
    code: String,
    tone: String,
    rows: { type: Array, default: () => [] },
    teachers: { type: Array, default: () => [] },
    busyTeacherIds: { type: Array, default: () => [] }
  }
}
</script>
