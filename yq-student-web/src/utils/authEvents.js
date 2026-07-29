export const STUDENT_AUTH_EXPIRED_EVENT = 'yq:student-auth-expired'

let authExpiredNotified = false

export function notifyStudentAuthExpired(redirect) {
  if (authExpiredNotified) return
  authExpiredNotified = true
  window.dispatchEvent(new CustomEvent(STUDENT_AUTH_EXPIRED_EVENT, {
    detail: { redirect }
  }))
}

export function resetStudentAuthExpiredNotification() {
  authExpiredNotified = false
}
