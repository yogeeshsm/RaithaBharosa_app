# Firebase Data Seeding Scripts

This directory contains scripts to populate your Firebase Firestore database with sample data for testing the Agricultural Labour Booking System.

## Prerequisites

1. **Node.js** installed on your system
2. **Firebase Admin SDK**: Install dependencies
   ```bash
   npm install firebase-admin
   ```
3. **Service Account Key**: Download from Firebase Console

## Setup Instructions

### Step 1: Get Firebase Service Account Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (RaithaBharosa)
3. Click the gear icon ⚙️ → **Project Settings**
4. Go to **Service Accounts** tab
5. Click **Generate New Private Key**
6. Save the downloaded JSON file as `serviceAccountKey.json` in this `scripts/` directory

### Step 2: Install Dependencies

```bash
cd scripts
npm install firebase-admin
```

### Step 3: Run the Seeding Script

```bash
node seed_labour_data.js
```

## What Gets Seeded

The script will create **12 sample labourer profiles** in Firestore with the following data:

### Labourer Profiles

| Name | Village | District | Skills | Pricing | Status |
|------|---------|----------|--------|---------|--------|
| Ravi Kumar | Kanakapura | Ramanagara | Ploughing, Sowing, Harvesting | ₹600/day | Available |
| Lakshmi Devi | Bidadi | Ramanagara | Weeding, Harvesting, Transplanting | ₹500/day | Available |
| Manjunath Gowda | Harohalli | Ramanagara | Ploughing, Irrigation, Pesticide Spraying | ₹700/day | Available |
| Suma Bai | Channapatna | Ramanagara | Weeding, Harvesting, Sowing | ₹70/hour | Available |
| Nagaraj Reddy | Magadi | Ramanagara | Ploughing, Harvesting, Threshing | ₹650/day | Available |
| Kavitha Rani | Ramanagara | Ramanagara | Transplanting, Weeding, Harvesting | ₹65/hour | Available |
| Basavaraj Patil | Kanakapura | Ramanagara | Irrigation, Pesticide Spraying, Fertilizer | ₹750/day | Available |
| Geetha Kumari | Bidadi | Ramanagara | Sowing, Weeding, Harvesting | ₹550/day | Booked |
| Suresh Babu | Harohalli | Ramanagara | Ploughing, Threshing, Harvesting | ₹680/day | Available |
| Anitha Devi | Channapatna | Ramanagara | Transplanting, Weeding, Harvesting | ₹68/hour | Available |
| Venkatesh Gowda | Magadi | Ramanagara | Irrigation, Pesticide Spraying, Ploughing | ₹720/day | Available |
| Pushpa Rani | Ramanagara | Ramanagara | Weeding, Harvesting, Sowing | ₹520/day | Available |

### Data Characteristics

- **Total Profiles**: 12
- **Available**: 11
- **Booked**: 1 (Geetha Kumari - available in 3 days)
- **Districts**: Ramanagara (all profiles in same district for easy testing)
- **Villages**: Kanakapura, Bidadi, Harohalli, Channapatna, Magadi, Ramanagara
- **Skills**: 
  - PLOUGHING
  - SOWING
  - HARVESTING
  - WEEDING
  - TRANSPLANTING
  - IRRIGATION
  - PESTICIDE_SPRAYING
  - THRESHING
  - FERTILIZER_APPLICATION
- **Pricing Types**:
  - Daily Wage: ₹500 - ₹750/day
  - Hourly Rate: ₹65 - ₹70/hour
- **Experience**: 5-18 years per skill
- **Ratings**: 4.4 - 4.9 stars
- **Completed Bookings**: 29 - 72 jobs

## Testing the App

After seeding the data:

1. **Install the APK** on your device
2. **Login** or create a new account
3. **Select "Farmer" role** in role selection screen
4. **Complete your farmer profile**
5. **Click the Labour tab** (4th icon in bottom navigation)
6. **You should see 12 labourers** in the search results!

### Test Scenarios

1. **Search by Skill**: Filter by specific skills (e.g., Ploughing, Harvesting)
2. **Filter by Wage**: Set wage range (e.g., ₹500-₹700)
3. **Filter by Rating**: Set minimum rating (e.g., 4.5 stars)
4. **Sort Results**: Try sorting by Rating, Wage, or Distance
5. **View Profile**: Click on a labourer to see full details
6. **Create Booking**: Book a labourer for specific work

## Firestore Structure

The data is stored in Firestore with the following structure:

```
labourer_profiles/
  ├── labourer_001/
  │   ├── userId: "labourer_001"
  │   ├── name: "Ravi Kumar"
  │   ├── phoneNumber: "9876543210"
  │   ├── age: 35
  │   ├── gender: "Male"
  │   ├── village: "Kanakapura"
  │   ├── district: "Ramanagara"
  │   ├── latitude: 12.5489
  │   ├── longitude: 77.4253
  │   ├── skills: ["PLOUGHING", "SOWING", "HARVESTING"]
  │   ├── experienceYears: {PLOUGHING: 10, SOWING: 8, HARVESTING: 12}
  │   ├── pricingType: "DAILY_WAGE"
  │   ├── dailyWage: 600
  │   ├── hourlyRate: null
  │   ├── profilePhotoUrls: []
  │   ├── availabilityStatus: "AVAILABLE"
  │   ├── futureAvailability: null
  │   ├── averageRating: 4.5
  │   ├── totalRatings: 23
  │   ├── completedBookings: 45
  │   ├── createdAt: 1715587200000
  │   ├── updatedAt: 1715587200000
  │   ├── lastAvailabilityUpdate: 1715587200000
  │   └── preferredLanguage: "kn"
  ├── labourer_002/
  │   └── ...
  └── ...
```

## Troubleshooting

### Error: "Cannot find module 'firebase-admin'"
**Solution**: Run `npm install firebase-admin` in the scripts directory

### Error: "Could not load the default credentials"
**Solution**: Make sure `serviceAccountKey.json` is in the scripts directory

### Error: "Permission denied"
**Solution**: Check that your Firebase service account has Firestore write permissions

### No data showing in app
**Solution**: 
1. Verify data was seeded successfully (check Firebase Console → Firestore)
2. Make sure app is using the same Firebase project
3. Check that `google-services.json` is up to date
4. Try pulling to refresh in the app

## Cleaning Up Test Data

To remove all seeded data from Firestore:

```javascript
// Run this in Firebase Console → Firestore → Query
// Or create a cleanup script
const batch = db.batch();
const snapshot = await db.collection('labourer_profiles')
  .where('userId', '>=', 'labourer_001')
  .where('userId', '<=', 'labourer_012')
  .get();

snapshot.docs.forEach(doc => {
  batch.delete(doc.ref);
});

await batch.commit();
```

## Adding More Data

To add more labourers, edit `seed_labour_data.js` and add new objects to the `labourers` array following the same structure.

### Example:

```javascript
{
  userId: 'labourer_013',
  name: 'Your Name',
  phoneNumber: '9876543222',
  age: 30,
  gender: 'Male',
  village: 'Your Village',
  district: 'Your District',
  latitude: 12.xxxx,
  longitude: 77.xxxx,
  skills: ['PLOUGHING', 'SOWING'],
  experienceYears: {
    PLOUGHING: 5,
    SOWING: 4
  },
  pricingType: 'DAILY_WAGE',
  dailyWage: 600,
  hourlyRate: null,
  profilePhotoUrls: [],
  availabilityStatus: 'AVAILABLE',
  futureAvailability: null,
  averageRating: 4.5,
  totalRatings: 10,
  completedBookings: 20,
  createdAt: Date.now(),
  updatedAt: Date.now(),
  lastAvailabilityUpdate: Date.now(),
  preferredLanguage: 'kn'
}
```

## Security Note

⚠️ **IMPORTANT**: Never commit `serviceAccountKey.json` to version control!

Add to `.gitignore`:
```
scripts/serviceAccountKey.json
scripts/node_modules/
```

## Support

If you encounter any issues:
1. Check Firebase Console for error logs
2. Verify Firestore security rules allow writes
3. Ensure service account has proper permissions
4. Check that the Firebase project ID matches your app

---

**Happy Testing!** 🎉
