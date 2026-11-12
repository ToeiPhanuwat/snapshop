import { NextResponse, type NextRequest } from "next/server";
import { isAdminAuthorization } from "@/lib/auth";

/** Asks the browser for the admin login on every /admin page (and the Server Actions posted to them). */
export function proxy(request: NextRequest) {
  if (isAdminAuthorization(request.headers.get("authorization"))) {
    return NextResponse.next();
  }
  return new NextResponse("ต้องเข้าสู่ระบบผู้ดูแล", {
    status: 401,
    headers: { "WWW-Authenticate": 'Basic realm="snapshop admin", charset="UTF-8"' },
  });
}

export const config = {
  matcher: ["/admin", "/admin/:path*"],
};
