const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_agent_123";
const BOB_UID = "bob_buyer_456";
const CHARLIE_UID = "charlie_other_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

function validPropertyPayload(ownerId, overrides = {}) {
  const ts = pastDate(10000);
  return {
    ownerId,
    title: "វីឡាទំនើប បឹងកេងកង ១",
    listingType: "SALE",
    propertyType: "VILLA",
    priceUsd: 285000,
    district: "បឹងកេងកង ១",
    city: "ភ្នំពេញ",
    bedrooms: 4,
    bathrooms: 5,
    areaSqm: 320,
    description: "វីឡាស្អាតទាន់សម័យ មានអាងហែលទឹក និងសួនច្បារ",
    imageKey: "villa_luxury",
    agentName: "សុខ ដារ៉ា",
    agentAgency: "ផ្ទះខ្មែរ អចលនទ្រព្យ",
    businessHotline: "012 888 999",
    telegramHandle: "@sokdara_khmerhome",
    visibility: "PUBLIC",
    status: "AVAILABLE",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("properties").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("properties").doc("prop_1").set(validPropertyPayload(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("agent_reviews").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("chats").where("participantIds", "array-contains", ALICE_UID).get());
});

test("Properties: supports APARTMENT category, partial updates, terminal CLOSED lock, and query parity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("properties").doc("prop_apt").set(
      validPropertyPayload(ALICE_UID, { propertyType: "APARTMENT", listingType: "RENT", priceUsd: 750 })
    )
  );
  await assertSucceeds(
    aliceDb.collection("properties").doc("prop_priv").set(
      validPropertyPayload(ALICE_UID, { visibility: "PRIVATE" })
    )
  );

  // Query parity
  await assertSucceeds(bobDb.collection("properties").where("visibility", "==", "PUBLIC").get());
  await assertSucceeds(aliceDb.collection("properties").where("ownerId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("properties").get());

  // Terminal CLOSED lock
  await assertSucceeds(
    aliceDb.collection("properties").doc("prop_apt").update({
      status: "CLOSED",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("properties").doc("prop_apt").update({
      status: "AVAILABLE",
      updatedAt: pastDate(500),
    })
  );
});

test("Agent reviews: client leaves 1-5 star review, edits own review; invalid rating & cross-user edit denied", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate(8000);

  await assertSucceeds(
    bobDb.collection("agent_reviews").doc("rev_1").set({
      agentId: ALICE_UID,
      agentName: "សុខ ដារ៉ា",
      reviewerId: BOB_UID,
      reviewerName: "បូបា",
      rating: 5,
      comment: "ភ្នាក់ងាររួសរាយ និងណែនាំផ្ទះល្អណាស់!",
      visibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invalid rating (>5 or 0) rejected
  await assertFails(
    bobDb.collection("agent_reviews").doc("rev_bad").set({
      agentId: ALICE_UID,
      agentName: "សុខ ដារ៉ា",
      reviewerId: BOB_UID,
      reviewerName: "បូបា",
      rating: 6,
      comment: "Invalid",
      visibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Scoped query succeeds; unfiltered query fails if private check isn't present—wait: visibility filter aligns
  await assertSucceeds(
    charlieDb.collection("agent_reviews")
      .where("visibility", "==", "PUBLIC")
      .where("agentId", "==", ALICE_UID)
      .get()
  );

  // Reviewer can update own rating/comment; Charlie cannot
  await assertSucceeds(
    bobDb.collection("agent_reviews").doc("rev_1").update({
      rating: 4,
      comment: "សេវាកម្មល្អ ឆ្លើយតបរហ័ស",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    charlieDb.collection("agent_reviews").doc("rev_1").update({
      rating: 1,
      updatedAt: pastDate(500),
    })
  );
});

test("Direct messaging: buyer initiates property chat, exchanges messages with agent, outsider denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(aliceDb.collection("properties").doc("prop_chat").set(validPropertyPayload(ALICE_UID)));

  const chatId = `prop_chat__${BOB_UID}__${ALICE_UID}`;
  const ts = pastDate(6000);

  // Deterministic existence check before creating the chat document
  await assertSucceeds(bobDb.collection("chats").doc(chatId).get());

  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).set({
      buyerId: BOB_UID,
      buyerName: "បូបា",
      agentId: ALICE_UID,
      agentName: "សុខ ដារ៉ា",
      propertyId: "prop_chat",
      propertyTitle: "វីឡាទំនើប បឹងកេងកង ១",
      lastMessage: "សួស្តី តើផ្ទះនេះនៅទំនេរទេ?",
      lastSenderId: BOB_UID,
      participantIds: [BOB_UID, ALICE_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      senderId: BOB_UID,
      senderName: "បូបា",
      text: "សួស្តី តើផ្ទះនេះនៅទំនេរទេ?",
      messageType: "TEXT",
      participantIds: [BOB_UID, ALICE_UID],
      createdAt: ts,
    })
  );

  // Agent replies and updates chat thread lastMessage
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_2").set({
      chatId,
      senderId: ALICE_UID,
      senderName: "សុខ ដារ៉ា",
      text: "បាទ នៅទំនេរ! តើអ្នកចង់ណាត់មើលថ្ងៃណាដែរ?",
      messageType: "TEXT",
      participantIds: [BOB_UID, ALICE_UID],
      createdAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).update({
      lastMessage: "បាទ នៅទំនេរ! តើអ្នកចង់ណាត់មើលថ្ងៃណាដែរ?",
      lastSenderId: ALICE_UID,
      updatedAt: pastDate(1000),
    })
  );

  // Scoped queries succeed
  await assertSucceeds(bobDb.collection("chats").where("participantIds", "array-contains", BOB_UID).get());
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages")
      .where("participantIds", "array-contains", BOB_UID)
      .get()
  );

  // Outsider Charlie denied on chat, unfiltered messages list, and individual message get
  await assertFails(charlieDb.collection("chats").doc(chatId).get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").get());
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_hack").set({
      chatId,
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      text: "Injected",
      messageType: "TEXT",
      participantIds: [BOB_UID, ALICE_UID, CHARLIE_UID],
      createdAt: pastDate(500),
    })
  );
});

test("Inquiries & Favorites: scoped reads/writes succeed and cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("properties").doc("prop_inq").set(validPropertyPayload(ALICE_UID)));

  // Favorites
  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).collection("favorites").get());
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("favorites").doc("prop_inq").set({
      userId: BOB_UID,
      propertyId: "prop_inq",
      createdAt: pastDate(1000),
    })
  );

  // Inquiry
  const ts = pastDate(5000);
  await assertSucceeds(
    bobDb.collection("inquiries").doc("inq_1").set({
      buyerId: BOB_UID,
      buyerName: "បូបា",
      buyerPhone: "099 123 456",
      agentId: ALICE_UID,
      agentName: "សុខ ដារ៉ា",
      propertyId: "prop_inq",
      propertyTitle: "វីឡាទំនើប បឹងកេងកង ១",
      inquiryType: "VIEWING",
      preferredDate: "ស្អែក ម៉ោង 10:00 ព្រឹក",
      message: "សូមអនុញ្ញាតមើលផ្ទះនេះនៅថ្ងៃស្អែក",
      status: "PENDING",
      participantIds: [BOB_UID, ALICE_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("inquiries").where("participantIds", "array-contains", ALICE_UID).get());
  await assertFails(aliceDb.collection("inquiries").get());
});
