import random

def get_available_hotels(location: str, check_in_date: str, check_out_date: str) -> list[dict]:
    """
    Get a list of available hotels in a given location for the specified dates.
    
    Args:
        location: The city or region to search for hotels (e.g., "Paris, France").
        check_in_date: The check-in date in YYYY-MM-DD format.
        check_out_date: The check-out date in YYYY-MM-DD format.
        
    Returns:
        A list of dictionaries, each containing hotel details like name, price_per_night, and rating.
    """
    # Mock data for demonstration
    return [
        {"name": f"Grand Plaza {location}", "price_per_night": 150, "rating": 4.5},
        {"name": f"Budget Inn {location}", "price_per_night": 60, "rating": 3.2},
        {"name": f"Luxury Resort {location}", "price_per_night": 350, "rating": 4.9},
    ]

def get_tourist_attractions(location: str) -> list[dict]:
    """
    Get a list of popular tourist attractions in a given location.
    
    Args:
        location: The city or region to search for attractions (e.g., "Kyoto, Japan").
        
    Returns:
        A list of dictionaries, each containing attraction details like name, description, and type.
    """
    # Mock data for demonstration
    attractions = [
        {"name": "Central Museum", "description": "Explore the rich history and art of the region.", "type": "Museum"},
        {"name": "Botanical Gardens", "description": "Beautiful landscapes and exotic plant species.", "type": "Nature"},
        {"name": "Historic City Center", "description": "Walking tours and historic architecture.", "type": "Sightseeing"},
        {"name": "Local Market", "description": "Experience local culture, street food, and crafts.", "type": "Shopping"},
    ]
    return random.sample(attractions, k=3)

def get_weather(location: str, date: str) -> dict:
    """
    Get the weather forecast for a specific location and date.
    
    Args:
        location: The city to check the weather for (e.g., "New York").
        date: The date for the forecast in YYYY-MM-DD format, or "today".
        
    Returns:
        A dictionary containing the weather condition and temperature in Celsius.
    """
    # Mock data for demonstration
    conditions = ["Sunny", "Partly Cloudy", "Rainy", "Thunderstorms", "Snow"]
    return {
        "condition": random.choice(conditions),
        "temperature_celsius": random.randint(-5, 35),
        "location": location,
        "date": date
    }

def get_travel_modes(origin: str, destination: str) -> list[dict]:
    """
    Get available modes of travel between an origin and a destination, along with estimated duration and cost.
    
    Args:
        origin: The starting location.
        destination: The final destination.
        
    Returns:
        A list of dictionaries, each containing the mode of transport, estimated duration, and approximate cost.
    """
    # Mock data for demonstration
    return [
        {"mode": "Flight", "duration": "2h 30m", "approx_cost": 250},
        {"mode": "Train", "duration": "5h 15m", "approx_cost": 80},
        {"mode": "Bus", "duration": "8h 45m", "approx_cost": 45},
        {"mode": "Car Rental", "duration": "6h 0m", "approx_cost": 120},
    ]
