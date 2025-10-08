export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  phoneNumber?: string;
  isActive: boolean;
}

export interface AuthResponse {
  success: boolean;
  token: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  password: string;
  role: number;
}

export enum UserRole {
  Admin = 0,
  EVOwner = 1,
  Operator = 2,
  BackofficeUser = 3
}

export enum ConnectorType {
  Type1 = 0,
  Type2 = 1,
  CHAdeMO = 2,
  CCS = 3,
  TeslaSuper = 4
}

export enum ConnectorStatus {
  Available = 0,
  Occupied = 1,
  Reserved = 2,
  OutOfOrder = 3,
  Maintenance = 4
}

export enum StationStatus {
  Active = 0,
  Inactive = 1,
  Maintenance = 2,
  OutOfOrder = 3
}

export interface Location {
  latitude: number;
  longitude: number;
}

export interface Connector {
  id: string;
  type: ConnectorType;
  power: number;
  isAvailable: boolean;
  status: ConnectorStatus;
}

export interface ChargingStation {
  id: string;
  name: string;
  description: string;
  location: Location;
  address: string;
  operatorId: string;
  connectors: Connector[];
  status: StationStatus;
  amenities: string[];
  openingHours: string;
  pricePerKWh: number;
  imageUrls: string[];
  createdAt: string;
  updatedAt: string;
  isDeleted: boolean;
}

export interface CreateStationRequest {
  name: string;
  description: string;
  location: Location;
  address: string;
  operatorId?: string;
  connectors: Omit<Connector, 'id'>[];
  amenities: string[];
  openingHours: string;
  pricePerKWh: number;
  imageUrls: string[];
}

// Booking related types and enums
export enum BookingStatus {
  Pending = 0,
  Confirmed = 1,
  InProgress = 2,
  Completed = 3,
  Cancelled = 4,
  NoShow = 5
}

export interface Booking {
  id: string;
  userId: string;
  stationId: string;
  connectorId: string;
  startTime: string;
  endTime: string;
  status: BookingStatus;
  qrCode: string;
  energyConsumed: number;
  totalCost: number;
  notes: string;
  confirmedBy?: string;
  confirmedAt?: string;
  createdAt: string;
  updatedAt: string;
  isDeleted: boolean;
}

export interface CreateBookingRequest {
  userId: string;
  stationId: string;
  connectorId: string;
  startTime: string;
  endTime: string;
  notes: string;
}

export interface UpdateBookingRequest {
  stationId: string;
  connectorId: string;
  startTime: string;
  endTime: string;
  notes?: string;
}

export interface CompleteBookingRequest {
  energyConsumed: number;
}