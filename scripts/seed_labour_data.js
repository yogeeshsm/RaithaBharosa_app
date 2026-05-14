/**
 * Firestore Data Seeding Script for Agricultural Labour Booking System
 * 
 * This script populates Firestore with sample labourer profiles for testing.
 * 
 * Prerequisites:
 * 1. Install Firebase Admin SDK: npm install firebase-admin
 * 2. Download service account key from Firebase Console
 * 3. Save as 'serviceAccountKey.json' in this directory
 * 
 * Usage:
 * node seed_labour_data.js
 */

const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Sample labourer profiles
const labourers = [
  {
    userId: 'labourer_001',
    name: 'Ravi Kumar',
    phoneNumber: '9876543210',
    age: 35,
    gender: 'Male',
    village: 'Kanakapura',
    district: 'Ramanagara',
    latitude: 12.5489,
    longitude: 77.4253,
    skills: ['PLOUGHING', 'SOWING', 'HARVESTING'],
    experienceYears: {
      PLOUGHING: 10,
      SOWING: 8,
      HARVESTING: 12
    },
    pricingType: 'DAILY_WAGE',
    dailyWage: 600,
    hourlyRate: null,
    profilePhotoUrls: [],
    availabilityStatus: 'AVAILABLE',
    futureAvailability: null,
    averageRating: 4.5,
    totalRatings: 23,
    completedBookings: 45,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    lastAvailabilityUpdate: Date.now(),
    preferredLanguage: 'kn'
  },
  {
    userId: 'labourer_002',
    name: 'Lakshmi Devi',
    phoneNumber: '9876543211',
    age: 28,
    gender: 'Female',
    village: 'Bidadi',
    district: 'Ramanagara',
    latitude: 12.7953,
    longitude: 77.3831,
    skills: ['WEEDING', 'HARVESTING', 'TRANSPLANTING'],
    experienceYears: {
      WEEDING: 6,
      HARVESTING: 7,
      TRANSPLANTING: 5
    },
    pricingType: 'DAILY_WAGE',
    dailyWage: 500,
    hourlyRate: null,
    profilePhotoUrls: [],
    availabilityStatus: 'AVAILABLE',
    futureAvailability: null,
    averageRating: 4.8,
    totalRatings: 31,
    completedBookings: 62,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    lastAvailabilityUpdate: Date.now(),
    preferredLanguage: 'kn'
  },
  {
    userId: 'labourer_003',
    name: 'Manjunath Gowda',
    phoneNumber: '9876543212',
    age: 42,
    gender: 'Male',
    village: 'Harohalli',
    district: 'Ramanagara',
    latitude: 12.7500,
    longitude: 77.4167,
    skills: ['PLOUGHING', 'IRRIGATION', 'PESTICIDE_SPRAYING'],
    experienceYears: {
      PLOUGHING: 15,
      IRRIGATION: 12,
      PESTICIDE_SPRAYING: 10
    },
    pricingType: 'DAILY_WAGE',
    dailyWage: 700,
    hourlyRate: null,
    profilePhotoUrls: [],
    availabilityStatus: 'AVAILABLE',
    futureAvailability: null,
    averageRating: 4.6,
    totalRatings: 28,
    completedBookings: 58,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    lastAvailabilityUpdate: Date.now(),
    preferredLanguage: 'kn'
  },
  {
    userId: 'labourer_004',
    name: 'Suma Bai',
    phoneNumber: '9876543213',
    age: 32,
    gender: 'Female',
    village: 'Channapatna',
    district: 'Ramanagara',
    latitude: 12.6515,
    longitude: 77.2066,
    skills: ['WEEDING', 'HARVESTING', 'SOWING'],
    experienceYears: {
      WEEDING: 8,
      HARVESTING: 9,
      SOWING: 7
    },
    pricingType: 'HOURLY_RATE',
    dailyWage: null,
    hourlyRate: 70,
    profilePhotoUrls: [],
    availabilityStatus: 'AVAILABLE',
    futureAvailability: null,
    averageRating: 4.7,
    totalRatings: 19,
    completedBookings: 38,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    lastAvailabilityUpdate: Date.now(),
    preferredLanguage: 'kn'
  },
  {
    userId: 'labourer_005',
    name: 'Nagaraj Reddy',
    phoneNumber: '9876543214',
    age: 38,
    gender: 'Male',
    village: 'Magadi',
    district: 'Ramanagara',
    latitude: 12.9577,
    longitude: 77.2244,
    skills: ['PLOUGHING', 'HARVESTING', 'THRESHING'],
    experienceYears: {
      PLOUGHING: 12,
      HARVESTING: 13,
      THRESHING: 10
    },
    pricingType: 'DAILY_WAGE',
    dailyWage: 650,
    hourlyRate: null,
    profilePhotoUrls: [],
    availabilityStatus: 'AVAILABLE',
    futureAvailability: null,
    averageRating: 4.4,
    totalRatings: 25,
    completedBookings: 51,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    lastAvailabilityUpdate: Date.now(),
    preferredLanguage: 'kn'
  },
  {
    userId: 'labourer_006',
    name: 'Kavitha Rani',
    phoneNumber: '9876543215',
    age: 26,
    gender: 'Female',
    village: 'Ramanagara',
    district: 'Ramanagara',
    latitude: 12.7175,
    longitude: 77.2806,
    skills: ['TRANSPLANTING', 'WEEDING', 'HARVESTING'],
    experienceYears: {
      TRANSPLANTING: 5,
      WEEDING: 6,
      HARVESTING: 5
    },
    pricingType: 'HOURLY_RATE',
    dailyWage: null,
    hourlyRate: 65,
    profilePhotoUrls: [],
    availabilityStatus: 'AVAILABLE',
    futureAvailability: null,
    averageRating: 4.9,
    totalRatings: 15,
    completedBookings: 29,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    lastAvailabilityUpdate: Date.now(),
    preferredLanguage: 'kn'
  },
  {
    userId: 'labourer_007',
    name: 'Basavaraj Patil',
    phoneNumber: '9876543216',
    age: 45,
    gender: 'Male',
    village: 'Kanakapura',
    district: 'Ramanagara',
    latitude: 12.5400,
    longitude: 77.4200,
    skills: ['IRRIGATION', 'PESTICIDE_SPRAYING', 'FERTILIZER_APPLICATION'],
    experienceYears: {
      IRRIGATION: 18,
      PESTICIDE_SPRAYING: 15,
      FERTILIZER_APPLICATION: 12
    },
    pricingType: 'DAILY_WAGE',
    dailyWage: 750,
    hourlyRate: null,
    profilePhotoUrls: [],
    availabilityStatus: 'AVAILABLE',
    futureAvailability: null,
    averageRating: 4.7,
    totalRatings: 34,
    completedBookings: 72,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    lastAvailabilityUpdate: Date.now(),
    preferredLanguage: 'kn'
  },
  {
    userId: 'labourer_008',
    name: 'Geetha Kumari',
    phoneNumber: '9876543217',
    age: 30,
    gender: 'Female',
    village: 'Bidadi',
    district: 'Ramanagara',
    latitude: 12.8000,
    longitude: 77.3900,
    skills: ['SOWING', 'WEEDING', 'HARVESTING'],
    experienceYears: {
      SOWING: 7,
      WEEDING: 8,
      HARVESTING: 7
    },
    pricingType: 'DAILY_WAGE',
    dailyWage: 550,
    hourlyRate: null,
    profilePhotoUrls: [],
    availabilityStatus: 'BOOKED',
    futureAvailability: Date.now() + (3 * 24 * 60 * 60 * 1000), // Available in 3 days
    averageRating: 4.6,
    totalRatings: 22,
    completedBookings: 44,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    lastAvailabilityUpdate: Date.now(),
    preferredLanguage: 'kn'
  },
  {
    userId: 'labourer_009',
    name: 'Suresh Babu',
    phoneNumber: '9876543218',
    age: 40,
    gender: 'Male',
    village: 'Harohalli',
    district: 'Ramanagara',
    latitude: 12.7600,
    longitude: 77.4100,
    skills: ['PLOUGHING', 'THRESHING', 'HARVESTING'],
    experienceYears: {
      PLOUGHING: 14,
      THRESHING: 12,
      HARVESTING: 15
    },
    pricingType: 'DAILY_WAGE',
    dailyWage: 680,
    hourlyRate: null,
    profilePhotoUrls: [],
    availabilityStatus: 'AVAILABLE',
    futureAvailability: null,
    averageRating: 4.5,
    totalRatings: 27,
    completedBookings: 56,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    lastAvailabilityUpdate: Date.now(),
    preferredLanguage: 'kn'
  },
  {
    userId: 'labourer_010',
    name: 'Anitha Devi',
    phoneNumber: '9876543219',
    age: 29,
    gender: 'Female',
    village: 'Channapatna',
    district: 'Ramanagara',
    latitude: 12.6600,
    longitude: 77.2100,
    skills: ['TRANSPLANTING', 'WEEDING', 'HARVESTING'],
    experienceYears: {
      TRANSPLANTING: 6,
      WEEDING: 7,
      HARVESTING: 6
    },
    pricingType: 'HOURLY_RATE',
    dailyWage: null,
    hourlyRate: 68,
    profilePhotoUrls: [],
    availabilityStatus: 'AVAILABLE',
    futureAvailability: null,
    averageRating: 4.8,
    totalRatings: 18,
    completedBookings: 35,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    lastAvailabilityUpdate: Date.now(),
    preferredLanguage: 'kn'
  },
  {
    userId: 'labourer_011',
    name: 'Venkatesh Gowda',
    phoneNumber: '9876543220',
    age: 36,
    gender: 'Male',
    village: 'Magadi',
    district: 'Ramanagara',
    latitude: 12.9500,
    longitude: 77.2300,
    skills: ['IRRIGATION', 'PESTICIDE_SPRAYING', 'PLOUGHING'],
    experienceYears: {
      IRRIGATION: 11,
      PESTICIDE_SPRAYING: 9,
      PLOUGHING: 10
    },
    pricingType: 'DAILY_WAGE',
    dailyWage: 720,
    hourlyRate: null,
    profilePhotoUrls: [],
    availabilityStatus: 'AVAILABLE',
    futureAvailability: null,
    averageRating: 4.6,
    totalRatings: 24,
    completedBookings: 49,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    lastAvailabilityUpdate: Date.now(),
    preferredLanguage: 'kn'
  },
  {
    userId: 'labourer_012',
    name: 'Pushpa Rani',
    phoneNumber: '9876543221',
    age: 33,
    gender: 'Female',
    village: 'Ramanagara',
    district: 'Ramanagara',
    latitude: 12.7200,
    longitude: 77.2850,
    skills: ['WEEDING', 'HARVESTING', 'SOWING'],
    experienceYears: {
      WEEDING: 9,
      HARVESTING: 10,
      SOWING: 8
    },
    pricingType: 'DAILY_WAGE',
    dailyWage: 520,
    hourlyRate: null,
    profilePhotoUrls: [],
    availabilityStatus: 'AVAILABLE',
    futureAvailability: null,
    averageRating: 4.7,
    totalRatings: 21,
    completedBookings: 42,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    lastAvailabilityUpdate: Date.now(),
    preferredLanguage: 'kn'
  }
];

// Function to seed data
async function seedLabourData() {
  console.log('🌱 Starting to seed labour data...\n');
  
  const batch = db.batch();
  let count = 0;
  
  for (const labourer of labourers) {
    const docRef = db.collection('labourer_profiles').doc(labourer.userId);
    batch.set(docRef, labourer);
    count++;
    console.log(`✅ Added: ${labourer.name} (${labourer.village}, ${labourer.district})`);
  }
  
  try {
    await batch.commit();
    console.log(`\n🎉 Successfully seeded ${count} labourer profiles to Firestore!`);
    console.log('\n📊 Summary:');
    console.log(`   - Total Labourers: ${count}`);
    console.log(`   - Available: ${labourers.filter(l => l.availabilityStatus === 'AVAILABLE').length}`);
    console.log(`   - Booked: ${labourers.filter(l => l.availabilityStatus === 'BOOKED').length}`);
    console.log(`   - Districts: ${[...new Set(labourers.map(l => l.district))].join(', ')}`);
    console.log(`   - Skills: PLOUGHING, SOWING, HARVESTING, WEEDING, TRANSPLANTING, IRRIGATION, PESTICIDE_SPRAYING, THRESHING, FERTILIZER_APPLICATION`);
    console.log('\n✨ You can now test the labour booking feature in the app!');
  } catch (error) {
    console.error('❌ Error seeding data:', error);
  }
  
  process.exit(0);
}

// Run the seeding
seedLabourData();
