from flask import Flask, request, jsonify

app = Flask(__name__)

# Crop suitability database
CROP_DATA = {
    "Rice":     {"N": (50, 120), "P": (25, 60),  "K": (30, 80),  "ph": (5.5, 7.0), "season": ["Kharif"]},
    "Wheat":    {"N": (80, 150), "P": (40, 80),  "K": (40, 100), "ph": (6.0, 7.5), "season": ["Rabi"]},
    "Maize":    {"N": (60, 120), "P": (30, 70),  "K": (30, 80),  "ph": (5.5, 7.0), "season": ["Kharif", "Zaid"]},
    "Cotton":   {"N": (50, 100), "P": (30, 60),  "K": (40, 80),  "ph": (6.0, 7.5), "season": ["Kharif"]},
    "Sugarcane":{"N":(100, 200), "P": (50, 100), "K":(100, 200), "ph": (6.0, 8.0), "season": ["Kharif", "Zaid"]}
}

def is_suitable(crop, N, P, K, ph, season):
    c = CROP_DATA[crop]
    return (c["N"][0] <= N <= c["N"][1] and
            c["P"][0] <= P <= c["P"][1] and
            c["K"][0] <= K <= c["K"][1] and
            c["ph"][0] <= ph <= c["ph"][1] and
            season in c["season"])

@app.route('/')
def home():
    return {"message": "Crop API is LIVE! (D:\\AndroidStudioProjects\\MyApplication2)"}

@app.route('/predict_crop', methods=['POST'])
def predict_crop():
    data = request.json
    N, P, K, ph = data['N'], data['P'], data['K'], data['ph']
    location, season = data['location'], data['season']

    suitable = [crop for crop in CROP_DATA if is_suitable(crop, N, P, K, ph, season)]

    if not suitable:
        return jsonify({
            "recommended_crop": "No Match",
            "reason": "Try different NPK, pH, or season.",
            "alternatives": []
        })

    recommended = suitable[0]
    alternatives = suitable[1:3] if len(suitable) > 1 else []

    reason = f"Optimal soil and season for {recommended.lower()}."

    return jsonify({
        "recommended_crop": recommended,
        "reason": reason,
        "alternatives": alternatives
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)