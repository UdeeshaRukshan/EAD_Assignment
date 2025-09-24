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
  OutOfOrder = 2,
  Reserved = 3
}

export enum StationStatus {
  Active = 0,
  Inactive = 1,
  Maintenance = 2
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