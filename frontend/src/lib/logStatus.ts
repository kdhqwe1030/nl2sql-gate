import type { LogStatus } from '../types/audit'

// AuditView(조회 기록 표)와 QuestionCard(지난 기록 카드)가 같이 쓰는 상태 라벨.
export const LOG_STATUS_LABEL: Record<LogStatus, { label: string; chip: string }> = {
  SUCCESS: { label: '정상', chip: 'chip-success' },
  CLARIFY: { label: '확인 필요', chip: 'chip-warning' },
  DENIED: { label: '권한 없음', chip: 'chip-danger' },
  BLOCKED: { label: '차단', chip: 'chip-danger' },
  ERROR: { label: '오류', chip: 'chip-danger' },
}
