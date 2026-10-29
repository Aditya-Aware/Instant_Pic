export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
  displayName: string;
}

export interface EventResponse {
  id: string;
  name: string;
  description: string | null;
  createdByUserId: string;
  startDate: string | null;
  endDate: string | null;
  status: string;
  createdAt: string;
}

export interface EventMemberResponse {
  id: string;
  eventId: string;
  userId: string;
  displayName: string;
  role: 'ORGANIZER' | 'PHOTOGRAPHER' | 'MEMBER';
  status: 'ACTIVE' | 'LEFT' | 'REMOVED';
  joinedAt: string;
}

export interface InvitationResponse {
  id: string;
  eventId: string;
  code: string;
  expiresAt: string;
  maxUses: number | null;
  usesCount: number;
  status: string;
}

export interface InvitationPreviewResponse {
  eventId: string;
  eventName: string;
  organizerDisplayName: string;
  memberCount: number;
  expiresAt: string;
}

export interface FaceEnrollmentResponse {
  status: 'ACTIVE' | 'DELETED' | 'NOT_ENROLLED';
  sampleCount: number;
  updatedAt: string | null;
}

export interface PhotoResponse {
  id: string;
  eventId: string;
  uploadedByMemberId: string;
  width: number | null;
  height: number | null;
  processingStatus: 'PENDING' | 'DETECTING' | 'MATCHING' | 'RULES_EVALUATED' | 'FAILED';
  galleryVisible: boolean;
  createdAt: string;
}

export interface SharingRuleResponse {
  id: string;
  ruleType: string;
  enabled: boolean;
  priority: number;
  configJson: string;
}

export interface DeliveryResponse {
  id: string;
  photoId: string;
  recipientMemberId: string;
  status: string;
  reason: string;
  ruleName: string;
  decidedAt: string;
  deliveredAt: string | null;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  fieldErrors: string[];
}
