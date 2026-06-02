## AI Usage

#### Which AI tools used
- Claude

#### Key Prompts/questions asked

## Prompt 1
```
Act as a software architect, summarize me the usages of Bundesbank exchange rate API based on the available endpoints. I need to understand which endpoints to use and how based on the use cases below:

- As a client, I want to get a list of all available currencies
- As a client, I want to get all EUR-FX exchange rates at all available dates as a collection
- As a client, I want to get the EUR-FX exchange rate at particular day
- As a client, I want to get a foreign exchange amount for a given currency converted to EUR on a particular day. 

Here is the available API documentation: [https://api.statistiken.bundesbank.de/doc/index.html?urls.primaryName=Deutsche+REST+API+Dokumentation](https://api.statistiken.bundesbank.de/doc/index.html?urls.primaryName=Deutsche+REST+API+Dokumentation)

Avoid using verbose output, be concise and clear regarding the usage details.
```

## Prompt 2
```
Act as a software engineer, how to structure the incremental load based on the implemented ExchangeRateLoader.java.
```


#### Relevant AI responses that shaped your solution
## Response for the 1st prompt
## Response

Here's the architect summary — all four use cases mapped to the relevant endpoints.

---

## Bundesbank Exchange Rate API — Use Case Mapping

**Base URL:** `https://api.statistiken.bundesbank.de/rest`  
**Dataflow for EUR-FX rates:** `BBEX3`  
**Key structure:** `{frequency}.{foreignCurrency}.{baseCurrency}.{source}.{type}.{variant}`  
**Daily EUR rates key pattern:** `D.{CCY}.EUR.BB.AC.000`

---

### UC1 — List all available currencies

There is no dedicated "list currencies" endpoint. The standard approach is to use the `serieskeyonly` detail parameter on a wildcard query — this returns all time series keys without fetching data, letting you extract the currency dimension from each key.

```
GET /data/BBEX3/D..EUR.BB.AC.000?detail=serieskeyonly
```

Parse the returned keys; the second segment is the 3-letter ISO currency code (e.g. `USD`, `JPY`, `CHF`).

---

### UC2 — All EUR-FX exchange rates at all available dates

Use the wildcard key with no date restriction. Expect a large payload — use `bbk_csv_zip` format to handle the volume.

```
GET /data/BBEX3/D..EUR.BB.AC.000
Accept: application/vnd.bbk.data+csv-zip;version=1.0.0
```

Or with explicit format param:
```
GET /data/BBEX3/D..EUR.BB.AC.000?format=bbk_csv_zip
```

---

### UC3 — EUR-FX exchange rate on a particular day

Pin both `startPeriod` and `endPeriod` to the same date (`YYYY-MM-DD`). Wildcard the currency to get all currencies for that day, or fix it for a single one.

**All currencies on a specific date:**
```
GET /data/BBEX3/D..EUR.BB.AC.000?startPeriod=2024-03-15&endPeriod=2024-03-15
```

**Single currency (e.g. USD) on a specific date:**
```
GET /data/BBEX3/D.USD.EUR.BB.AC.000?startPeriod=2024-03-15&endPeriod=2024-03-15
```

---

### UC4 — Convert a foreign amount to EUR on a particular day

The API provides **rates only** — it does not perform conversion calculations. The workflow is:

1. Fetch the rate for the target currency and date using UC3.
2. Apply the conversion client-side: `amountInEUR = foreignAmount / rate`

> The rates in `BBEX3` are expressed as units of foreign currency per 1 EUR, so dividing inverts the direction correctly (e.g. a rate of `1.08` for USD means 1 EUR = 1.08 USD → 100 USD / 1.08 ≈ 92.59 EUR).

---
### Prompt 2
Partial response that shows the logic evolution how to make an initial and incremental load.
- load state empty?          → FULL LOAD   (fetch all history, no date filter)
- lastLoadedDate < today?    → INCREMENTAL LOAD (fetch from lastLoadedDate - overlapDays → today)
- lastLoadedDate == today?   → SKIP        (no API call)
- any exception?             → FAILED      (state not updated, next run retries)


#### Reasoning for the accepting, modifyig, or rejecting AI suggestions

- UC1 and UC4 are relatively usable explanations that shaped the implementation details. 
- UC2 and UC3 as proposals look reasonable. However, especially the UC2 is not feasible to implement due to the size of the load that can be expected. That's why the idea has been accepted to use the endpoint, but the implementation has been moved to the idea of having persisted data in the db rather than just having a call and return the response to the requester. An initial load on start up would persist the information in db. UC3 is also shaped with the decision for UC2 which includes the persisted data in db.  