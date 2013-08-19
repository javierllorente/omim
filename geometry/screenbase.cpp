#include "screenbase.hpp"

#include "../std/cmath.hpp"
#include "../base/matrix.hpp"
#include "../base/logging.hpp"
#include "transformations.hpp"
#include "angles.hpp"

ScreenBase::ScreenBase() :
    m_PixelRect(0, 0, 640, 480),
    m_Scale(0.1),
    m_Angle(0.0),
    m_Org(320, 240),
    m_GlobalRect(m_Org, ang::AngleD(0), m2::RectD(-320, -240, 320, 240)),
    m_ClipRect(m2::RectD(0, 0, 640, 480))
{
  m_GtoP = math::Identity<double, 3>();
  m_PtoG = math::Identity<double, 3>();
//  UpdateDependentParameters();
}

ScreenBase::ScreenBase(m2::RectI const & pxRect, m2::AnyRectD const & glbRect)
{
  OnSize(pxRect);
  SetFromRect(glbRect);
}

void ScreenBase::UpdateDependentParameters()
{
  m_PtoG = math::Shift( /// 5. shifting on (E0, N0)
               math::Rotate( /// 4. rotating on the screen angle
                   math::Scale( /// 3. scaling to translate pixel sizes to global
                       math::Scale( /// 2. swapping the Y axis??? why??? supposed to be a rotation on -pi / 2 here.
                           math::Shift( /// 1. shifting for the pixel center to become (0, 0)
                               math::Identity<double, 3>(),
                               - m_PixelRect.Center()
                               ),
                           1,
                           -1
                           ),
                       m_Scale,
                       m_Scale
                       ),
                   m_Angle.cos(),
                   m_Angle.sin()
               ),
               m_Org
           );

  m_GtoP = math::Inverse(m_PtoG);

  m2::PointD const pxC = m_PixelRect.Center();
  double const szX = PtoG(m2::PointD(m_PixelRect.maxX(), pxC.y)).Length(PtoG(m2::PointD(pxC)));
  double const szY = PtoG(m2::PointD(pxC.x, m_PixelRect.minY())).Length(PtoG(m2::PointD(pxC)));

  m_GlobalRect = m2::AnyRectD(m_Org, m_Angle, m2::RectD(-szX, -szY, szX, szY));
  m_ClipRect = m_GlobalRect.GetGlobalRect();
}

void ScreenBase::SetFromRects(m2::AnyRectD const & glbRect, m2::RectD const & pxRect)
{
  double hScale = glbRect.GetLocalRect().SizeX() / pxRect.SizeX();
  double vScale = glbRect.GetLocalRect().SizeY() / pxRect.SizeY();

  m_Scale = max(hScale, vScale);
  m_Angle = glbRect.Angle();
  m_Org = glbRect.GlobalCenter();

  UpdateDependentParameters();
}

void ScreenBase::SetFromRect(m2::AnyRectD const & glbRect)
{
  SetFromRects(glbRect, m_PixelRect);
}

void ScreenBase::SetOrg(m2::PointD const & p)
{
  m_Org = p;
  UpdateDependentParameters();
}

void ScreenBase::Move(double dx, double dy)
{
  m_Org = PtoG(GtoP(m_Org) - m2::PointD(dx, dy));
  UpdateDependentParameters();
}

void ScreenBase::MoveG(m2::PointD const & p)
{
  m_Org -= p;
  UpdateDependentParameters();
}

void ScreenBase::Scale(double scale)
{
  m_Scale /= scale;
  UpdateDependentParameters();
}

void ScreenBase::Rotate(double angle)
{
  m_Angle = ang::AngleD(m_Angle.val() + angle);
  UpdateDependentParameters();
}

void ScreenBase::OnSize(m2::RectI const & r)
{
  m_PixelRect = m2::RectD(r);
  UpdateDependentParameters();
}

void ScreenBase::OnSize(int x0, int y0, int w, int h)
{
  OnSize(m2::RectI(x0, y0, x0 + w, y0 + h));
}

math::Matrix<double, 3, 3> const & ScreenBase::GtoPMatrix() const
{
  return m_GtoP;
}

math::Matrix<double, 3, 3> const & ScreenBase::PtoGMatrix() const
{
  return m_PtoG;
}

m2::RectD const & ScreenBase::PixelRect() const
{
  return m_PixelRect;
}

m2::AnyRectD const & ScreenBase::GlobalRect() const
{
  return m_GlobalRect;
}

m2::RectD const & ScreenBase::ClipRect() const
{
  return m_ClipRect;
}

double ScreenBase::GetMinPixelRectSize() const
{
  return min(m_PixelRect.SizeX(), m_PixelRect.SizeY());
}

double ScreenBase::GetScale() const
{
  return m_Scale;
}

double ScreenBase::GetAngle() const
{
  return m_Angle.val();
}

void ScreenBase::SetAngle(double angle)
{
  m_Angle = ang::AngleD(angle);
  UpdateDependentParameters();
}

m2::PointD const & ScreenBase::GetOrg() const
{
  return m_Org;
}

int ScreenBase::GetWidth() const
{
  return my::rounds(m_PixelRect.SizeX());
}

int ScreenBase::GetHeight() const
{
  return my::rounds(m_PixelRect.SizeY());
}

math::Matrix<double, 3, 3> const ScreenBase::CalcTransform(m2::PointD const & oldPt1, m2::PointD const & oldPt2,
                                                           m2::PointD const & newPt1, m2::PointD const & newPt2)
{
  double s = newPt1.Length(newPt2) / oldPt1.Length(oldPt2);
  double a = ang::AngleTo(newPt1, newPt2) - ang::AngleTo(oldPt1, oldPt2);

  math::Matrix<double, 3, 3> m =
      math::Shift(
          math::Scale(
              math::Rotate(
                  math::Shift(
                      math::Identity<double, 3>(),
                      -oldPt1.x, -oldPt1.y
                      ),
                  a
                  ),
              s, s
              ),
          newPt1.x, newPt1.y
          );
  return m;
}

void ScreenBase::SetGtoPMatrix(math::Matrix<double, 3, 3> const & m)
{
  m_GtoP = m;
  m_PtoG = math::Inverse(m_GtoP);
  /// Extracting transformation params, assuming that the matrix
  /// somehow represent a valid screen transformation
  /// into m_PixelRectangle
  double dx, dy, a, s;
  ExtractGtoPParams(m, a, s, dx, dy);
  m_Angle = ang::AngleD(-a);
  m_Scale = 1 / s;
  m_Org = PtoG(m_PixelRect.Center());

  UpdateDependentParameters();
}

void ScreenBase::GtoP(m2::RectD const & glbRect, m2::RectD & pxRect) const
{
  pxRect = m2::RectD(GtoP(glbRect.LeftTop()), GtoP(glbRect.RightBottom()));
}

void ScreenBase::PtoG(m2::RectD const & pxRect, m2::RectD & glbRect) const
{
  glbRect = m2::RectD(PtoG(pxRect.LeftTop()), PtoG(pxRect.RightBottom()));
}

void ScreenBase::GetTouchRect(m2::PointD const & pixPoint, double pixRadius,
                              m2::AnyRectD & glbRect) const
{
  double const r = pixRadius * m_Scale;
  glbRect = m2::AnyRectD(PtoG(pixPoint), m_Angle, m2::RectD(-r, -r, r, r));
}

bool IsPanningAndRotate(ScreenBase const & s1, ScreenBase const & s2)
{
  m2::RectD const & r1 = s1.GlobalRect().GetLocalRect();
  m2::RectD const & r2 = s2.GlobalRect().GetLocalRect();

  m2::PointD c1 = r1.Center();
  m2::PointD c2 = r2.Center();

  m2::PointD globPt(c1.x - r1.minX(),
                    c1.y - r1.minY());

  m2::PointD p1 = s1.GtoP(s1.GlobalRect().ConvertFrom(c1)) - s1.GtoP(s1.GlobalRect().ConvertFrom(c1 + globPt));
  m2::PointD p2 = s2.GtoP(s2.GlobalRect().ConvertFrom(c2)) - s2.GtoP(s2.GlobalRect().ConvertFrom(c2 + globPt));

  return p1.EqualDxDy(p2, 0.00001);
}

void ScreenBase::ExtractGtoPParams(math::Matrix<double, 3, 3> const & m,
                                   double & a, double & s, double & dx, double & dy)
{
  s = sqrt(m(0, 0) * m(0, 0) + m(0, 1) * m(0, 1));

  a = ang::AngleIn2PI(atan2(-m(0, 1), m(0, 0)));

  dx = m(2, 0);
  dy = m(2, 1);
}
