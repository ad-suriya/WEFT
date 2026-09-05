export const BLOCK_QUOTES: readonly string[] = [
  "You're getting distracted. The task you chose to focus on is still waiting.",
  "This app can wait. The work you set out to do can't finish itself.",
  "Every minute here is a minute you promised to your work session.",
  "Discipline is choosing between what you want now and what you want most.",
  "You didn't block this app by accident — you blocked it because it pulls you away.",
  "Future you is counting on present you to stay on task.",
  "The urge to check this will pass. The work you skip won't do itself.",
  "You're one tap away from getting back to what actually matters right now.",
  "Distraction feels productive for a second and costs you the next twenty minutes.",
  "Small interruptions like this one are how deep work quietly dies.",
];

export function randomBlockQuote(): string {
  return BLOCK_QUOTES[Math.floor(Math.random() * BLOCK_QUOTES.length)];
}
