/** A compact emoji set grouped for the chat picker. Native characters: no dependency, no image assets. */
export interface EmojiGroup { key: 'smileys' | 'gestures' | 'hearts' | 'study' | 'travel', icon: string, emojis: string[] }

export const EMOJI_GROUPS: EmojiGroup[] = [
  {
    key: 'smileys', icon: '😀',
    emojis: ['😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '🙂', '😉', '😍', '🥰', '😘', '😎', '🤩', '🥳', '🤗', '🤔', '😐',
      '😴', '😢', '😭', '😅', '😮', '😱', '😬', '🙄', '😇', '🤝'],
  },
  {
    key: 'gestures', icon: '👍',
    emojis: ['👍', '👎', '👌', '✌️', '🤞', '🤟', '👏', '🙌', '🙏', '💪', '👋', '🤙', '☝️', '👀', '🫶', '🤌', '✋', '👊', '🫡', '🤲'],
  },
  {
    key: 'hearts', icon: '❤️',
    emojis: ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '💔', '💯', '🔥', '✨', '⭐', '🌟', '🎉', '🎊', '✅', '❌', '⚠️', '❓'],
  },
  {
    key: 'study', icon: '🎓',
    emojis: ['🎓', '📚', '📖', '✏️', '📝', '📄', '📎', '📅', '⏰', '💼', '🏫', '🏆', '🥇', '💡', '🧠', '💻', '📧', '📞', '🔔', '📌'],
  },
  {
    key: 'travel', icon: '✈️',
    emojis: ['✈️', '🛫', '🛬', '🌍', '🌏', '🗺️', '🧳', '🛂', '🏠', '🏙️', '🚆', '🚌', '🚕', '☀️', '🌧️', '❄️', '🍜', '🍚', '☕', '🍎'],
  },
]
