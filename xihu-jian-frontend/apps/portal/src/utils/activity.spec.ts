import { describe, expect, it } from 'vitest'
import { activityStatusLabel, registrationStatusLabel } from './activity'

describe('activity status labels', () => {
  it('maps activity status to Chinese label', () => {
    expect(activityStatusLabel('PUBLISHED')).toBe('已发布')
    expect(activityStatusLabel('DRAFT')).toBe('草稿')
  })
  it('maps registration availability', () => {
    expect(registrationStatusLabel('OPEN')).toBe('报名开放中')
    expect(registrationStatusLabel('FULL')).toBe('名额已满')
  })
})
