package com.example.evmobile

import org.json.JSONArray
import org.json.JSONObject

// Test class to verify API parsing logic
class ApiParsingTest {
    
    fun testStationParsing() {
        // Sample API response from your backend
        val apiResponse = """
        [
            {
                "name": "Downtown Charging Hub",
                "description": "Fast charging station in downtown area",
                "location": {
                    "latitude": 34.0522,
                    "longitude": -118.2437
                },
                "address": "123 Main Street, Downtown, CA 90210",
                "operatorId": "",
                "connectors": [
                    {
                        "id": "68d3cde42a683eb15d86752f",
                        "type": 1,
                        "power": 0,
                        "isAvailable": true,
                        "status": 0
                    },
                    {
                        "id": "68d3cde42a683eb15d867530",
                        "type": 3,
                        "power": 0,
                        "isAvailable": true,
                        "status": 0
                    }
                ],
                "status": 2,
                "amenities": ["WiFi", "Parking", "Restroom"],
                "openingHours": "06:00-22:00",
                "pricePerKWh": 0.35,
                "imageUrls": [],
                "id": "68d3cde42a683eb15d867531",
                "createdAt": "2025-09-24T10:54:28.535Z",
                "updatedAt": "2025-09-24T11:24:11.121Z",
                "isDeleted": false
            }
        ]
        """.trimIndent()
        
        println("🧪 Testing API Response Parsing")
        println("================================")
        
        try {
            val stations = parseStationsJSON(apiResponse)
            println("✅ Parsing successful!")
            println("Parsed ${stations.size} station(s)")
            
            stations.forEach { station ->
                println("\n📍 Station: ${station.name}")
                println("   Address: ${station.address}")
                println("   Location: ${station.latitude}, ${station.longitude}")
                println("   Active: ${station.isActive}")
                println("   Connectors: ${station.connectorTypes.joinToString(", ")}")
                println("   Slots: ${station.availableSlots}/${station.totalSlots}")
                println("   Price: Rs. ${String.format("%.2f", station.pricePerKwh)}/kWh")
                println("   Hours: ${station.operatingHours}")
                println("   Amenities: ${station.amenities.joinToString(", ")}")
                println("   Rating: ${String.format("%.1f", station.rating)}")
            }
            
        } catch (e: Exception) {
            println("❌ Parsing failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun parseStationsJSON(jsonString: String): List<ChargingStationModel> {
        val stations = mutableListOf<ChargingStationModel>()
        
        try {
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val stationJson = jsonArray.getJSONObject(i)
                
                // Parse location object
                val locationJson = stationJson.getJSONObject("location")
                val latitude = locationJson.getDouble("latitude")
                val longitude = locationJson.getDouble("longitude")
                
                // Parse connectors array
                val connectorsArray = stationJson.optJSONArray("connectors")
                val connectorTypes = parseConnectorTypes(connectorsArray)
                val (totalSlots, availableSlots) = calculateSlots(connectorsArray)
                
                // Determine if station is active based on status
                val status = stationJson.optInt("status", 0)
                val isActive = status != 3 && !stationJson.optBoolean("isDeleted", false)
                
                val station = ChargingStationModel(
                    id = stationJson.getString("id"),
                    name = stationJson.getString("name"),
                    address = stationJson.getString("address"),
                    latitude = latitude,
                    longitude = longitude,
                    isActive = isActive,
                    connectorTypes = connectorTypes,
                    pricePerKwh = stationJson.optDouble("pricePerKWh", 0.0),
                    operatingHours = stationJson.optString("openingHours", "24/7"),
                    amenities = parseAmenities(stationJson.optJSONArray("amenities")),
                    rating = calculateRating(),
                    totalSlots = totalSlots,
                    availableSlots = availableSlots
                )
                
                stations.add(station)
            }
        } catch (e: Exception) {
            throw Exception("Failed to parse stations data: ${e.message}")
        }
        
        return stations
    }
    
    private fun parseConnectorTypes(connectorsArray: JSONArray?): List<String> {
        val connectorTypes = mutableListOf<String>()
        
        connectorsArray?.let { array ->
            for (i in 0 until array.length()) {
                val connector = array.getJSONObject(i)
                val typeNumber = connector.optInt("type", 1)
                val typeName = when (typeNumber) {
                    1 -> "AC Type 1"
                    2 -> "AC Type 2"
                    3 -> "DC Fast"
                    4 -> "CHAdeMO"
                    5 -> "CCS"
                    else -> "AC"
                }
                if (!connectorTypes.contains(typeName)) {
                    connectorTypes.add(typeName)
                }
            }
        }
        
        return if (connectorTypes.isEmpty()) listOf("AC") else connectorTypes
    }
    
    private fun parseAmenities(amenitiesArray: JSONArray?): List<String> {
        val amenities = mutableListOf<String>()
        
        amenitiesArray?.let { array ->
            for (i in 0 until array.length()) {
                amenities.add(array.getString(i))
            }
        }
        
        return amenities
    }
    
    private fun calculateSlots(connectorsArray: JSONArray?): Pair<Int, Int> {
        var totalSlots = 0
        var availableSlots = 0
        
        connectorsArray?.let { array ->
            totalSlots = array.length()
            for (i in 0 until array.length()) {
                val connector = array.getJSONObject(i)
                val isAvailable = connector.optBoolean("isAvailable", false)
                if (isAvailable) {
                    availableSlots++
                }
            }
        }
        
        return Pair(maxOf(totalSlots, 1), availableSlots)
    }
    
    private fun calculateRating(): Double {
        return 3.0 + (kotlin.random.Random.nextDouble() * 2.0)
    }
}