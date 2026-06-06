import client from './client'

export const startGitScan = (projectName, githubUrl) =>
  client.post('/api/scans/git', { projectName, sourceType: 'git', githubUrl })

export const startZipScan = (projectName, file) => {
  const formData = new FormData()
  formData.append('projectName', projectName)
  formData.append('file', file)
  return client.post('/api/scans/zip', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const getScanStatus = (id) =>
  client.get(`/api/scans/${id}/status`)

export const getScanHistory = () =>
  client.get('/api/scans')

export const getScan = (id) =>
  client.get(`/api/scans/${id}`)

export const deleteScan = (id) =>
  client.delete(`/api/scans/${id}`)

export const getFindings = (scanId) =>
  client.get(`/api/scans/${scanId}/findings`)

export const updateTriage = (scanId, findingId, triageStatus) =>
  client.patch(`/api/scans/${scanId}/findings/${findingId}/triage`, { triageStatus })

export const updateNote = (scanId, findingId, note) =>
  client.patch(`/api/scans/${scanId}/findings/${findingId}/note`, { note })
