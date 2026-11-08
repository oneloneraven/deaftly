# Prompt Requirements Document for Deaftly

## Overview
This PRD describes the concept for Deaftly, a mobile app for deaf people which facilitates quick, simple, and low friction communication with non-deaf people.

## Concept
Deaftly should be a fast, minimal but beautifully designed mobile application which offers deaf people an intuitive and engaging way to communicate with non-deaf people. Features include:

### Quick-access text templates
Pre-canned (editable), as well as custom. These can include placeholders which can be filled in by tapping them, again from pre-defined choices or ad-hoc. This provides a deaf person with a quick way to choose/compose a message and hold up their phone to a non-deaf to read. 

Example: "Hi! I'm deaf and I'd like to check in. You can {placeholder} to communicate with me." where placeholder could be a pre-canned options like "type on your phone", or a custom one like "use interpretive dance"

### Single screen chat between two people
A single screen which allows two people to have a conversation through messages. Each person would be allocated a side of the screen for message bubbles, along with a chosen (or default) colour, and optional name and/or avatar. The conversation would be turn based and a swipe control would determine which person (side) is typing.

This interface should be easy to read and also easy to inject personality into, such as easy formatting, highlighting, quoting, etc.


### Live OCR for rapid insertion of external text into quick access text templates forus in restaurants, shops, etc.
Building ont the quick access text templates, live OCR would use the phone's camera to read text which the user can easily select and insert into a template. 

Example (browsing a restarant menu): "Hi, I'd like to order food. Could I have the {OCR placeholder} {+touch hint for add more} please?" where the OCR placeholder would open the camera and allow the user to select some text (drag over characters, or touch to pick recognised word groups like "Pasta arrabiata") from the menu.

## Brief
Execute this chain of thought where each step builds on the information gathered and outputs created from the previous one. Ask for any clarifications, additional information, and decisions as you go. Once you have everything you need for a step, present a summary and wait for approval before continuining to the next.
1. Task: Analyse the concept described in this document and ask for more information where you need it. 
   Goal: You fully understand the concept
   Output: A set of high-level requirements
2. Task: Research any sources you can find by or for deaf people regarding communicating through text on a phone with non-deaf people. This includes typing to converse, as well as showing text on a phone to a non-deaf person to tell them something.
   Goal: Understand the perspective of deaf people to inform our design decisions
   Output: An updated set of high-level requirements, and/or an appendix to them with this information
3. Task: Create a set of mockups for this concept
   Goal: Being a design discussion about the layout and style of this concept
   Output: A realistic visual design of this concept as it would appear on a phone
4. Task: Decide the tech stack
   Goal: Based on the requirements and visual design, select an appropriate tech stack on which to build this concept
   Output: A proposed toolchain including development, test, security, and CI/CD
5. Task: Plan the delivery
   Goal: Break down the requirements into Epics, Features, and Stories which can delivered by a developer/agent 
   Output: A detailed product backlog with a structured delivery plan from zero to MVP
   
