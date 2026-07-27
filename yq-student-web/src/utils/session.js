const TOKEN_KEY = 'yq_student_token'
const SIGN_SECRET_KEY = 'yq_student_sign_secret'
const PROFILE_KEY = 'yq_student_profile'

export function saveStudentSession(loginData) {
  localStorage.setItem(TOKEN_KEY, loginData.token)
  localStorage.setItem(SIGN_SECRET_KEY, loginData.signSecret)
  localStorage.setItem(PROFILE_KEY, JSON.stringify(loginData.studentDetailsVO || {}))
}

export function clearStudentSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(SIGN_SECRET_KEY)
  localStorage.removeItem(PROFILE_KEY)
}

export function getStudentToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getStudentSignSecret() {
  return localStorage.getItem(SIGN_SECRET_KEY)
}

export function hasStudentSession() {
  return Boolean(getStudentToken() && getStudentSignSecret())
}

export function getStudentProfile() {
  try {
    return JSON.parse(localStorage.getItem(PROFILE_KEY) || '{}')
  } catch (error) {
    clearStudentSession()
    return {}
  }
}

export function saveStudentProfile(profile) {
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile || {}))
}
