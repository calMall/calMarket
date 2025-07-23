import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";
interface LoginInfo {
  userInfo: UserStore | null;
  setUserInfo: (user: UserStore) => void;
  logout: () => void;
}
const UserStore = create<LoginInfo>()(
  persist<LoginInfo>(
    (set) => ({
      userInfo: null,
      setUserInfo: (user: UserStore) => set({ userInfo: user }),
      logout: () => set({ userInfo: null }),
    }),
    { name: "userStore", storage: createJSONStorage(() => sessionStorage) }
  )
);
export default UserStore;
