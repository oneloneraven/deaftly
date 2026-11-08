# Appendix A – Research: Deaf Perspectives on Phone-Based Text Communication

Status: Approved (2026-11-07)
Feeds into: [requirements.md](requirements.md)

## Scope and method

This is desk research into how deaf people use text on a phone to communicate face to face with hearing people. It covers both typing back and forth and showing a message. Sources are UK deaf organisations (RNID, Deaf Action), UK government user research (DWP, Home Office), news reports, app reviews, and community forums.

**Limitations.**
- There is little first-hand or academic research specifically about holding up a phone to show text. Most of the evidence is guidance, anecdote and app reviews.
- Several of the incident reports are from the US. The legal context differs from the UK's, but the behaviour reported is the same.
- None of this replaces testing with deaf users. See the recommendation at the end.

## Findings

### F1. Written English is often a second language
Many BSL users consider BSL their first language and English their second ([Home Office](https://ukhomeoffice.github.io/accessibility-posters/deaf)). DWP user research found that BSL users read English much as people read a foreign language: they scan for familiar words and recognise a smaller vocabulary. Key modifiers get lost. In one example, "approximately" reversed the meaning of a question. Dense text is often skipped ([DWP Digital](https://dwpdigital.blog.gov.uk/2016/07/14/designing-for-deaf-people-when-english-is-the-second-language/)). English proficiency varies widely between individuals ([Limping Chicken](https://limpingchicken.com/2021/03/19/clare-vale-making-written-communication-accessible-for-deaf-customers-and-clients/)).

### F2. Writing anxiety, and choosing beats typing
DWP found that participants worried spelling mistakes would hide their meaning or make them look unintelligent. Offering choices, such as tick boxes and selections, removed that risk. Short free text felt much less daunting once part of the answer had already been chosen. Participants also wanted to check their writing before it was seen ([DWP Digital](https://dwpdigital.blog.gov.uk/2016/07/14/designing-for-deaf-people-when-english-is-the-second-language/)).

### F3. Hearing people's impatience is a real barrier
There are documented cases of staff refusing written orders, writing dismissive notes, or getting impatient ([ABC News 4](https://abcnews4.com/news/nation-world/deaf-woman-denied-service-at-oklahoma-burger-king), [ClickOrlando](https://www.clickorlando.com/news/2015/03/10/deaf-woman-says-port-orange-post-office-discriminated-against-her), [Tyla](https://tyla.com/news/ashley-vallejo-dutch-bros-coffee-deaf-receipt-note-20220525), [Yokohama City](https://en.city.yokohama.lg.jp/kenko-iryo-fukushi/fukushi-kaigo/fukushi/sabetsu/jireikensaku/omise/chokaku/0074.html)). One commentary notes that refusals usually look like impatience rather than hostility ([Isshindo](https://isshindo.blogspot.com/2026/03/ot-when-words-fall-short.html?m=1)). Forum users also report that hearing people don't respond in the way they've been asked to ([Mayo Clinic Connect](https://connect.mayoclinic.org/comment/1247527/replies)).

### F4. Text to show: big, simple, high contrast, quick to clear
Text-to-show apps are well used and well rated. Make It Big has around 3.8K ratings averaging 4.8 on the US App Store. Users value the simple black and white display with no ads. Their complaints are that multi-line text is hard to edit, and that the Android version auto-shrinks text, which limits it to short sentences. One reviewer prefers Cardzilla because a shake clears the text so they can keep typing ([Sorenson](https://sorenson.com/blog/vrs/the-best-apps-for-deaf-people-in-2025/), [WorldsApps](https://worldsapps.com/reviews-make-it-big), [Easter Seals Tech](https://www.eastersealstech.com/2017/03/17/am227-make-big-app/)). Big text also lets people communicate at a distance, for example from 2 metres ([Deaf Action](https://deafaction.org/info-and-advice/deaf-awareness-tips/)).

### F5. Speech-to-text is the standard tool for the reply direction
RNID and Deaf Action both recommend speech-to-text apps such as Google Live Transcribe for the hearing person's side ([RNID](https://rnid.org.uk/information-and-support/hearing-loss/communication-tips/), [Deaf Action](https://deafaction.org/info-and-advice/deaf-awareness-tips/)). Guides also suggest having the person type on a phone when speech isn't working ([Chicago MOPD](https://www.chicago.gov/content/dam/city/depts/mopd/supp_info/accessibility_guide_tools/deaf_low_vision/Communication%20Tips%20-%20Deaf%20and%20Hard%20of%20Hearing.docx)).

### F6. Preferences vary, so personalisation matters
Every guidance source stresses asking the individual how they prefer to communicate ([RNID](https://rnid.org.uk/information-and-support/hearing-loss/communication-tips/), [Deaf Children Australia](https://www.deafchildrenaustralia.org.au/wp-content/uploads/2022/09/Tips-for-communicating-1.pdf)). RNID's communication card exists so a deaf person can tell others how to communicate with them.

### F7. Preparing in advance helps
Guidance for deaf shoppers suggests writing the order out in advance and using a notes app ([Ark Times](https://blog.blog.wordpress.arktimes.com/?p=24451), [Lifeprint](https://www.lifeprint.com/asl101/topics/restaurant-ordering-food-without-voicing.htm)).

## Design implications

| Finding | Implication | Requirement change |
|---------|-------------|--------------------|
| F1, F2 | Composing should favour choosing over typing. Placeholders with preset options are the main route. | T9 (new) |
| F1 | Built-in template copy should use plain English: short sentences, common words, one idea per sentence. | T10 (new) |
| F3, F6 | Templates should tell the hearing person how to respond, briefly and politely. This acts as a built-in communication card. | T11 (new) |
| F2 | The user previews the message before showing it. | S1 amended |
| F4 | Text fills the screen down to a minimum legible size, then wraps and scrolls. It must not shrink until unreadable. | S2 amended |
| F4 | Show mode uses a simple, high-contrast colour scheme that the user can choose. | S6 (new) |
| F4, F7 | A free-text "type and show" entry point one tap from launch, with a one-tap clear. | S7 (new) |
| F3, F5 | When the turn switches to the hearing person, a short plain prompt tells them what to do. | H8 (new) |
| F1 | Highlighting supports emphasising key words such as quantities, times and negatives. | H4 already covers this; no change |
| F5 | Speech-to-text is the norm for replies. A cheap interim step is a one-tap hand-off to an installed captioning app. | Post-MVP [#67](https://github.com/ravendarque/deaftly/issues/67) (new) |
| F1 | BSL video content for templates or onboarding. | Post-MVP [#68](https://github.com/ravendarque/deaftly/issues/68) (new) |

## Recommendation

Test with deaf users, including BSL-first users, as early as the mockups (step 3) and again before the MVP release. The evidence about holding a phone up to a hearing person is thin, and the decisions that matter most (template wording, the show mode layout, chat orientation) are exactly where desk research runs out. Recruiting through a UK deaf organisation or community group is the obvious route. This will need a decision when we plan delivery in step 5.
