/**
 * Signing out asks first, so a stray click never ends a session mid-form. Every "Sign out" control calls `ask()`;
 * `SignOutDialog` (mounted once per layout) owns the answer and runs the real `signOut()` only on confirmation.
 */
export function useSignOutConfirm() {
  const open = useState<boolean>('sign-out-confirm', () => false)
  return {
    open,
    ask: () => { open.value = true },
    dismiss: () => { open.value = false },
  }
}
